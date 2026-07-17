package africa.credresearch.modules.review.infrastructure.persistence;

import africa.credresearch.modules.review.domain.model.ReviewComment;
import africa.credresearch.modules.review.domain.model.ReviewDecision;
import africa.credresearch.modules.review.domain.model.ReviewRequest;
import africa.credresearch.modules.review.domain.port.ReviewRepository;
import africa.credresearch.modules.review.infrastructure.persistence.entity.ReviewCommentEntity;
import africa.credresearch.modules.review.infrastructure.persistence.entity.ReviewDecisionEntity;
import africa.credresearch.modules.review.infrastructure.persistence.entity.ReviewRequestEntity;
import africa.credresearch.modules.review.infrastructure.persistence.repository.ReviewCommentJpaRepository;
import africa.credresearch.modules.review.infrastructure.persistence.repository.ReviewDecisionJpaRepository;
import africa.credresearch.modules.review.infrastructure.persistence.repository.ReviewRequestJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewRequestJpaRepository requests;
    private final ReviewCommentJpaRepository comments;
    private final ReviewDecisionJpaRepository decisions;

    public ReviewRepositoryAdapter(ReviewRequestJpaRepository requests, ReviewCommentJpaRepository comments,
                                   ReviewDecisionJpaRepository decisions) {
        this.requests = requests;
        this.comments = comments;
        this.decisions = decisions;
    }

    @Override
    public ReviewRequest createRequest(ReviewRequest r) {
        ReviewRequestEntity e = new ReviewRequestEntity();
        e.setDocumentId(r.documentId());
        e.setDocumentSectionId(r.documentSectionId());
        e.setRequestedBy(r.requestedBy());
        e.setReviewerUserId(r.reviewerUserId());
        e.setReviewerEmail(r.reviewerEmail());
        e.setStatus(r.status() == null ? "PENDING" : r.status());
        e.setNote(r.note());
        return toRequest(requests.save(e));
    }

    @Override
    public Optional<ReviewRequest> findRequest(UUID id) {
        return requests.findById(id).map(ReviewRepositoryAdapter::toRequest);
    }

    @Override
    public List<ReviewRequest> findRequestsByDocument(UUID documentId) {
        return requests.findByDocumentIdOrderByCreatedAtDesc(documentId).stream().map(ReviewRepositoryAdapter::toRequest).toList();
    }

    @Override
    public List<ReviewRequest> findInbox(UUID reviewerUserId) {
        return requests.findByReviewerUserIdAndStatusOrderByCreatedAtDesc(reviewerUserId, "PENDING")
                .stream().map(ReviewRepositoryAdapter::toRequest).toList();
    }

    @Override
    public ReviewRequest updateStatus(UUID id, String status, Instant decidedAt) {
        ReviewRequestEntity e = requests.findById(id).orElseThrow();
        e.setStatus(status);
        e.setDecidedAt(decidedAt);
        return toRequest(requests.save(e));
    }

    @Override
    public ReviewComment addComment(ReviewComment c) {
        ReviewCommentEntity e = new ReviewCommentEntity();
        e.setReviewRequestId(c.reviewRequestId());
        e.setAuthorUserId(c.authorUserId());
        e.setAuthorLabel(c.authorLabel());
        e.setAnchorStart(c.anchorStart());
        e.setAnchorEnd(c.anchorEnd());
        e.setQuote(c.quote());
        e.setBody(c.body());
        e.setResolved(c.resolved());
        return toComment(comments.save(e));
    }

    @Override
    public List<ReviewComment> findComments(UUID reviewRequestId) {
        return comments.findByReviewRequestIdOrderByCreatedAtAsc(reviewRequestId).stream().map(ReviewRepositoryAdapter::toComment).toList();
    }

    @Override
    public ReviewComment setResolved(UUID commentId, boolean resolved) {
        ReviewCommentEntity e = comments.findById(commentId).orElseThrow();
        e.setResolved(resolved);
        return toComment(comments.save(e));
    }

    @Override
    public Optional<ReviewComment> findComment(UUID commentId) {
        return comments.findById(commentId).map(ReviewRepositoryAdapter::toComment);
    }

    @Override
    public ReviewDecision addDecision(ReviewDecision d) {
        ReviewDecisionEntity e = new ReviewDecisionEntity();
        e.setReviewRequestId(d.reviewRequestId());
        e.setDecision(d.decision());
        e.setSummary(d.summary());
        e.setDecidedBy(d.decidedBy());
        return toDecision(decisions.save(e));
    }

    @Override
    public List<ReviewDecision> findDecisions(UUID reviewRequestId) {
        return decisions.findByReviewRequestIdOrderByCreatedAtAsc(reviewRequestId).stream().map(ReviewRepositoryAdapter::toDecision).toList();
    }

    private static ReviewRequest toRequest(ReviewRequestEntity e) {
        return new ReviewRequest(e.getId(), e.getDocumentId(), e.getDocumentSectionId(), e.getRequestedBy(),
                e.getReviewerUserId(), e.getReviewerEmail(), e.getStatus(), e.getNote(), e.getCreatedAt(), e.getDecidedAt());
    }

    private static ReviewComment toComment(ReviewCommentEntity e) {
        return new ReviewComment(e.getId(), e.getReviewRequestId(), e.getAuthorUserId(), e.getAuthorLabel(),
                e.getAnchorStart(), e.getAnchorEnd(), e.getQuote(), e.getBody(), e.isResolved(), e.getCreatedAt());
    }

    private static ReviewDecision toDecision(ReviewDecisionEntity e) {
        return new ReviewDecision(e.getId(), e.getReviewRequestId(), e.getDecision(), e.getSummary(),
                e.getDecidedBy(), e.getCreatedAt());
    }
}
