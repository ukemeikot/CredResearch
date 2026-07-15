package africa.credresearch.modules.project.infrastructure.persistence.repository;

import africa.credresearch.modules.project.infrastructure.persistence.entity.ActivityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, UUID> {

    List<ActivityEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
}
