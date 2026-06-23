package africa.credresearch.modules.project.infrastructure.persistence;

import africa.credresearch.modules.project.domain.model.Milestone;
import africa.credresearch.modules.project.domain.port.MilestoneRepository;
import africa.credresearch.modules.project.infrastructure.persistence.entity.MilestoneEntity;
import africa.credresearch.modules.project.infrastructure.persistence.repository.MilestoneJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MilestoneRepositoryAdapter implements MilestoneRepository {

    private final MilestoneJpaRepository jpa;

    public MilestoneRepositoryAdapter(MilestoneJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Milestone add(Milestone milestone) {
        MilestoneEntity e = new MilestoneEntity();
        e.setProjectId(milestone.projectId());
        e.setTitle(milestone.title());
        e.setDueDate(milestone.dueDate());
        e.setStatus(milestone.status() == null ? "PENDING" : milestone.status());
        e.setCompletedAt(milestone.completedAt());
        return toDomain(jpa.save(e));
    }

    @Override
    public List<Milestone> findByProject(UUID projectId) {
        return jpa.findByProjectIdAndDeletedAtIsNullOrderByDueDateAsc(projectId).stream()
                .map(MilestoneRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countByProject(UUID projectId) {
        return jpa.countByProjectIdAndDeletedAtIsNull(projectId);
    }

    @Override
    public long countByProjectAndStatus(UUID projectId, String status) {
        return jpa.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, status);
    }

    @Override
    public List<Milestone> findUpcomingByProject(UUID projectId) {
        return jpa.findByProjectIdAndCompletedAtIsNullAndDeletedAtIsNullOrderByDueDateAsc(projectId).stream()
                .map(MilestoneRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Milestone> findDueForReminder(LocalDate asOf) {
        return jpa.findByCompletedAtIsNullAndDeletedAtIsNullAndDueDateLessThanEqual(asOf).stream()
                .map(MilestoneRepositoryAdapter::toDomain).toList();
    }

    static Milestone toDomain(MilestoneEntity e) {
        return new Milestone(e.getId(), e.getProjectId(), e.getTitle(), e.getDueDate(),
                e.getStatus(), e.getCompletedAt());
    }
}
