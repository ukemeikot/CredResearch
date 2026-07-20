package africa.credresearch.modules.review.infrastructure.persistence.repository;

import africa.credresearch.modules.review.infrastructure.persistence.entity.ReviewRequestEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRequestJpaRepository extends JpaRepository<ReviewRequestEntity, UUID> {
    List<ReviewRequestEntity> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    List<ReviewRequestEntity> findByReviewerUserIdAndStatusOrderByCreatedAtDesc(UUID reviewerUserId, String status);
    java.util.Optional<ReviewRequestEntity> findByReviewTokenHashAndTokenExpiresAtAfter(String reviewTokenHash, java.time.Instant now);
}
