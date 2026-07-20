package africa.credresearch.modules.review.application;

import africa.credresearch.common.config.CredResearchProperties;
import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.notification.NotificationPort;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.common.util.TokenHasher;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.document.domain.port.DocumentSectionRepository;
import africa.credresearch.modules.identity.application.ProfileService;
import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import africa.credresearch.modules.review.domain.model.ReviewComment;
import africa.credresearch.modules.review.domain.model.ReviewDecision;
import africa.credresearch.modules.review.domain.model.ReviewRequest;
import africa.credresearch.modules.review.domain.port.ReviewRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supervisor review loop (Phase 6, FR-SUP-3..8): submit a section/document for review, leave
 * inline comments (resolvable threads), and record a decision that moves the request through its
 * status. Access is gated on membership of the document's project. (Magic-link external reviewers
 * and notifications are layered in the next increment.)
 */
@Service
public class ReviewService {

    private static final Set<String> DECISIONS = Set.of("APPROVED", "NEEDS_REVISION", "REJECTED");

    public record ReviewThread(ReviewRequest request, List<ReviewComment> comments, List<ReviewDecision> decisions) {}

    /** What a magic-link (account-less) reviewer sees: the request, the shared section, and threads. */
    public record ExternalView(ReviewRequest request, String sectionHeading, String sectionContent,
                               List<ReviewComment> comments, List<ReviewDecision> decisions) {}

    private final ReviewRepository reviews;
    private final DocumentRepository documents;
    private final DocumentSectionRepository sections;
    private final ProjectAccessGuard projectAccess;
    private final ProfileService profiles;
    private final UserRepository users;
    private final NotificationPort notifications;
    private final CredResearchProperties props;

    public ReviewService(ReviewRepository reviews, DocumentRepository documents, DocumentSectionRepository sections,
                         ProjectAccessGuard projectAccess, ProfileService profiles, UserRepository users,
                         NotificationPort notifications, CredResearchProperties props) {
        this.reviews = reviews;
        this.documents = documents;
        this.sections = sections;
        this.projectAccess = projectAccess;
        this.profiles = profiles;
        this.users = users;
        this.notifications = notifications;
        this.props = props;
    }

    /** Submit a section (or whole document) for review by a project member (FR-SUP-3). */
    @Transactional
    public ReviewRequest submit(UUID documentId, UUID sectionId, UUID reviewerUserId, String note) {
        requireDocumentMember(documentId);
        if (reviewerUserId == null) {
            throw ApiException.badRequest("REVIEWER_REQUIRED", "Choose a reviewer for this request.");
        }
        UUID me = TenantContextHolder.require().userId();
        ReviewRequest r = new ReviewRequest(null, documentId, sectionId, me, reviewerUserId, null,
                "PENDING", note, null, null);
        ReviewRequest created = reviews.createRequest(r);
        // Notify the internal reviewer (FR-SUP-9), best-effort.
        users.findById(reviewerUserId).ifPresent(u -> notifyQuietly(u.email(),
                "A document was shared with you for review",
                "You have a new review request on CredResearch."
                        + (note == null || note.isBlank() ? "" : " Note: " + note)
                        + " Open " + props.app().baseUrl() + " to review."));
        return created;
    }

    /**
     * Invite an external (account-less) supervisor by email (FR-SUP-1/2). Creates a review request
     * with a scoped, single-review magic-link token (opaque, stored hashed, 14-day expiry) and
     * emails the link. The token can only view/comment/decide on this one review.
     */
    @Transactional
    public ReviewRequest submitExternal(UUID documentId, UUID sectionId, String email, String note) {
        requireDocumentMember(documentId);
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "Enter the reviewer's email.");
        }
        UUID me = TenantContextHolder.require().userId();
        ReviewRequest created = reviews.createRequest(new ReviewRequest(
                null, documentId, sectionId, me, null, email.trim(), "PENDING", note, null, null));
        String raw = TokenHasher.randomToken();
        reviews.setReviewToken(created.id(), TokenHasher.sha256(raw), Instant.now().plus(14, ChronoUnit.DAYS));
        String link = props.app().baseUrl() + "/review/" + raw;
        notifyQuietly(email.trim(), "You've been asked to review a document on CredResearch",
                "A researcher has asked for your review."
                        + (note == null || note.isBlank() ? "" : " Note: " + note)
                        + " Review it here (no account needed): " + link
                        + " This link expires in 14 days.");
        return created;
    }

    /** All review requests for a document, each with its comments + decision history (FR-SUP-6). */
    public List<ReviewThread> listForDocument(UUID documentId) {
        requireDocumentMember(documentId);
        return reviews.findRequestsByDocument(documentId).stream()
                .map(req -> new ReviewThread(req, reviews.findComments(req.id()), reviews.findDecisions(req.id())))
                .toList();
    }

    /** A reviewer's pending queue across all their students (FR-SUP-7). */
    public List<ReviewRequest> inbox() {
        return reviews.findInbox(TenantContextHolder.require().userId());
    }

    /** Leave an inline comment, optionally anchored to a text range (FR-SUP-4). */
    @Transactional
    public ReviewComment comment(UUID reviewRequestId, String body, Integer anchorStart, Integer anchorEnd, String quote) {
        ReviewRequest req = requireRequestAccess(reviewRequestId);
        if (body == null || body.isBlank()) {
            throw ApiException.badRequest("EMPTY_COMMENT", "A comment cannot be empty.");
        }
        UUID me = TenantContextHolder.require().userId();
        String label = safeName();
        return reviews.addComment(new ReviewComment(null, req.id(), me, label, anchorStart, anchorEnd, quote,
                body, false, null));
    }

    @Transactional
    public ReviewComment resolveComment(UUID commentId, boolean resolved) {
        ReviewComment c = reviews.findComment(commentId)
                .orElseThrow(() -> ApiException.notFound("COMMENT_NOT_FOUND", "Comment not found"));
        requireRequestAccess(c.reviewRequestId());
        return reviews.setResolved(commentId, resolved);
    }

    /** Record a decision and move the request's status; history is preserved (FR-SUP-5/6). */
    @Transactional
    public ReviewThread decide(UUID reviewRequestId, String decision, String summary) {
        ReviewRequest req = requireRequestAccess(reviewRequestId);
        String d = decision == null ? "" : decision.toUpperCase();
        if (!DECISIONS.contains(d)) {
            throw ApiException.badRequest("INVALID_DECISION", "Decision must be APPROVED, NEEDS_REVISION or REJECTED.");
        }
        UUID me = TenantContextHolder.require().userId();
        reviews.addDecision(new ReviewDecision(null, req.id(), d, summary, me, null));
        ReviewRequest updated = reviews.updateStatus(req.id(), d, Instant.now());
        notifyRequesterOfDecision(updated, d, summary);
        return new ReviewThread(updated, reviews.findComments(req.id()), reviews.findDecisions(req.id()));
    }

    // ── Magic-link external review (token-scoped; no account, single review) ──────
    public ExternalView accessByToken(String rawToken) {
        ReviewRequest req = requireToken(rawToken);
        return externalView(req);
    }

    @Transactional
    public ExternalView externalComment(String rawToken, String body, Integer anchorStart, Integer anchorEnd, String quote) {
        ReviewRequest req = requireToken(rawToken);
        if (body == null || body.isBlank()) {
            throw ApiException.badRequest("EMPTY_COMMENT", "A comment cannot be empty.");
        }
        String label = req.reviewerEmail() == null ? "External reviewer" : req.reviewerEmail();
        reviews.addComment(new ReviewComment(null, req.id(), null, label, anchorStart, anchorEnd, quote, body, false, null));
        users.findById(req.requestedBy()).ifPresent(u -> notifyQuietly(u.email(),
                "New review comment on your document", label + " left a comment: " + body));
        return externalView(req);
    }

    @Transactional
    public ExternalView externalDecide(String rawToken, String decision, String summary) {
        ReviewRequest req = requireToken(rawToken);
        String d = decision == null ? "" : decision.toUpperCase();
        if (!DECISIONS.contains(d)) {
            throw ApiException.badRequest("INVALID_DECISION", "Decision must be APPROVED, NEEDS_REVISION or REJECTED.");
        }
        reviews.addDecision(new ReviewDecision(null, req.id(), d, summary, null, null));
        ReviewRequest updated = reviews.updateStatus(req.id(), d, Instant.now());
        notifyRequesterOfDecision(updated, d, summary);
        return externalView(updated);
    }

    private ReviewRequest requireToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.badRequest("INVALID_TOKEN", "Missing review link token.");
        }
        return reviews.findByValidToken(TokenHasher.sha256(rawToken), Instant.now())
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN",
                        "This review link is invalid or has expired."));
    }

    private ExternalView externalView(ReviewRequest req) {
        String heading = null;
        String content = null;
        if (req.documentSectionId() != null) {
            DocumentSection s = sections.findById(req.documentSectionId()).orElse(null);
            if (s != null) {
                heading = s.heading();
                content = s.content();
            }
        }
        return new ExternalView(req, heading, content,
                reviews.findComments(req.id()), reviews.findDecisions(req.id()));
    }

    /** Re-open a request for another round after revision (FR-SUP-8); prior decisions stay in history. */
    @Transactional
    public ReviewRequest resubmit(UUID reviewRequestId, String note) {
        ReviewRequest req = requireRequestAccess(reviewRequestId);
        if (note != null && !note.isBlank()) {
            // keep the latest note by recording it on a fresh PENDING round via status reset
        }
        return reviews.updateStatus(req.id(), "PENDING", null);
    }

    // ── access helpers ─────────────────────────────────────────────────────────
    private Document requireDocumentMember(UUID documentId) {
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));
        projectAccess.requireMember(doc.projectId());
        return doc;
    }

    private ReviewRequest requireRequestAccess(UUID reviewRequestId) {
        ReviewRequest req = reviews.findRequest(reviewRequestId)
                .orElseThrow(() -> ApiException.notFound("REVIEW_NOT_FOUND", "Review request not found"));
        requireDocumentMember(req.documentId());
        return req;
    }

    private String safeName() {
        try {
            return profiles.currentUser().fullName();
        } catch (RuntimeException e) {
            return "Reviewer";
        }
    }

    private void notifyRequesterOfDecision(ReviewRequest req, String decision, String summary) {
        users.findById(req.requestedBy()).ifPresent(u -> notifyQuietly(u.email(),
                "Your document review: " + decision.replace('_', ' '),
                "A reviewer recorded a decision of " + decision.replace('_', ' ') + "."
                        + (summary == null || summary.isBlank() ? "" : " Summary: " + summary)
                        + " Open " + props.app().baseUrl() + " to see the details."));
    }

    /** Best-effort email notification — a delivery failure must never break the review action. */
    private void notifyQuietly(String to, String subject, String body) {
        try {
            if (to != null && !to.isBlank()) {
                notifications.sendEmail(to, subject, body);
            }
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(ReviewService.class)
                    .warn("Review notification to {} failed (non-fatal): {}", to, e.toString());
        }
    }
}
