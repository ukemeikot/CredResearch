package africa.credresearch.modules.review.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.identity.application.ProfileService;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import africa.credresearch.modules.review.domain.model.ReviewComment;
import africa.credresearch.modules.review.domain.model.ReviewDecision;
import africa.credresearch.modules.review.domain.model.ReviewRequest;
import africa.credresearch.modules.review.domain.port.ReviewRepository;
import java.time.Instant;
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

    private final ReviewRepository reviews;
    private final DocumentRepository documents;
    private final ProjectAccessGuard projectAccess;
    private final ProfileService profiles;

    public ReviewService(ReviewRepository reviews, DocumentRepository documents,
                         ProjectAccessGuard projectAccess, ProfileService profiles) {
        this.reviews = reviews;
        this.documents = documents;
        this.projectAccess = projectAccess;
        this.profiles = profiles;
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
        return reviews.createRequest(r);
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
        return new ReviewThread(updated, reviews.findComments(req.id()), reviews.findDecisions(req.id()));
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
}
