package africa.credresearch.modules.review.infrastructure.persistence.repository;

import africa.credresearch.modules.review.infrastructure.persistence.entity.ReviewCommentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentJpaRepository extends JpaRepository<ReviewCommentEntity, UUID> {
    List<ReviewCommentEntity> findByReviewRequestIdOrderByCreatedAtAsc(UUID reviewRequestId);
}
