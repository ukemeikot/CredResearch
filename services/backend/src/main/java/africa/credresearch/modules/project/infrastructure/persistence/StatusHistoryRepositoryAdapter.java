package africa.credresearch.modules.project.infrastructure.persistence;

import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.port.StatusHistoryRepository;
import africa.credresearch.modules.project.infrastructure.persistence.entity.StatusHistoryEntity;
import africa.credresearch.modules.project.infrastructure.persistence.repository.StatusHistoryJpaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StatusHistoryRepositoryAdapter implements StatusHistoryRepository {

    private final StatusHistoryJpaRepository jpa;

    public StatusHistoryRepositoryAdapter(StatusHistoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void record(UUID projectId, ProjectStatus fromStatus, ProjectStatus toStatus, UUID changedBy) {
        StatusHistoryEntity e = new StatusHistoryEntity();
        e.setProjectId(projectId);
        e.setFromStatus(fromStatus == null ? null : fromStatus.name());
        e.setToStatus(toStatus.name());
        e.setChangedBy(changedBy);
        jpa.save(e);
    }
}
