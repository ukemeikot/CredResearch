package africa.credresearch.modules.project.infrastructure.persistence.repository;

import africa.credresearch.modules.project.infrastructure.persistence.entity.ProjectMemberEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberJpaRepository extends JpaRepository<ProjectMemberEntity, UUID> {

    Optional<ProjectMemberEntity> findByProjectIdAndUserIdAndDeletedAtIsNull(UUID projectId, UUID userId);

    List<ProjectMemberEntity> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(UUID projectId, UUID userId);

    long countByProjectIdAndDeletedAtIsNull(UUID projectId);
}
