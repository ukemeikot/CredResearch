package africa.credresearch.modules.review.domain.port;

import africa.credresearch.modules.review.domain.model.ReviewComment;
import africa.credresearch.modules.review.domain.model.ReviewDecision;
import africa.credresearch.modules.review.domain.model.ReviewRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    ReviewRequest createRequest(ReviewRequest r);
    Optional<ReviewRequest> findRequest(UUID id);
    List<ReviewRequest> findRequestsByDocument(UUID documentId);
    List<ReviewRequest> findInbox(UUID reviewerUserId);
    ReviewRequest updateStatus(UUID id, String status, Instant decidedAt);

    ReviewComment addComment(ReviewComment c);
    List<ReviewComment> findComments(UUID reviewRequestId);
    ReviewComment setResolved(UUID commentId, boolean resolved);
    Optional<ReviewComment> findComment(UUID commentId);

    ReviewDecision addDecision(ReviewDecision d);
    List<ReviewDecision> findDecisions(UUID reviewRequestId);
}
