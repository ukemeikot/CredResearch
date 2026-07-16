package africa.credresearch.modules.project.infrastructure.persistence.repository;

import africa.credresearch.modules.project.infrastructure.persistence.entity.StatusHistoryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryJpaRepository extends JpaRepository<StatusHistoryEntity, UUID> {
}
