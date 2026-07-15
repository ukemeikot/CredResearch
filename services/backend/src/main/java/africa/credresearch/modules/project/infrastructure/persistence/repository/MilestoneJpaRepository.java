package africa.credresearch.modules.project.infrastructure.persistence.repository;

import africa.credresearch.modules.project.infrastructure.persistence.entity.MilestoneEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneJpaRepository extends JpaRepository<MilestoneEntity, UUID> {

    List<MilestoneEntity> findByProjectIdAndDeletedAtIsNullOrderByDueDateAsc(UUID projectId);

    long countByProjectIdAndDeletedAtIsNull(UUID projectId);

    long countByProjectIdAndStatusAndDeletedAtIsNull(UUID projectId, String status);

    List<MilestoneEntity> findByProjectIdAndCompletedAtIsNullAndDeletedAtIsNullOrderByDueDateAsc(UUID projectId);

    List<MilestoneEntity> findByCompletedAtIsNullAndDeletedAtIsNullAndDueDateLessThanEqual(LocalDate asOf);
}
