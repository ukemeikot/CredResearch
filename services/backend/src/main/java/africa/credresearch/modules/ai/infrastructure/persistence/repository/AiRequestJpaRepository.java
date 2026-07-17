package africa.credresearch.modules.ai.infrastructure.persistence.repository;

import africa.credresearch.modules.ai.infrastructure.persistence.entity.AiRequestEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestJpaRepository extends JpaRepository<AiRequestEntity, UUID> {
    long countByUserIdAndStatusAndCreatedAtAfter(UUID userId, String status, Instant after);
}
