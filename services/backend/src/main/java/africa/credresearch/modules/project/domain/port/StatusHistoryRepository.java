package africa.credresearch.modules.project.domain.port;

import africa.credresearch.modules.project.domain.ProjectStatus;
import java.util.UUID;

public interface StatusHistoryRepository {

    void record(UUID projectId, ProjectStatus fromStatus, ProjectStatus toStatus, UUID changedBy);
}
