package africa.credresearch.modules.paper.infrastructure.persistence.repository;

import africa.credresearch.modules.paper.infrastructure.persistence.entity.PaperEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperJpaRepository extends JpaRepository<PaperEntity, UUID> {
    List<PaperEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
