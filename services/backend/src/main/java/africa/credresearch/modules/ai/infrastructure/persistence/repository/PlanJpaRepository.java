package africa.credresearch.modules.ai.infrastructure.persistence.repository;

import africa.credresearch.modules.ai.infrastructure.persistence.entity.PlanEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanJpaRepository extends JpaRepository<PlanEntity, UUID> {
    Optional<PlanEntity> findByCode(String code);
}
