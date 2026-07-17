package africa.credresearch.modules.ai.infrastructure.persistence.repository;

import africa.credresearch.modules.ai.infrastructure.persistence.entity.AiResponseEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiResponseJpaRepository extends JpaRepository<AiResponseEntity, UUID> {}
