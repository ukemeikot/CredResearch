package africa.credresearch.modules.review.infrastructure.persistence.repository;

import africa.credresearch.modules.review.infrastructure.persistence.entity.ReviewDecisionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDecisionJpaRepository extends JpaRepository<ReviewDecisionEntity, UUID> {
    List<ReviewDecisionEntity> findByReviewRequestIdOrderByCreatedAtAsc(UUID reviewRequestId);
}
