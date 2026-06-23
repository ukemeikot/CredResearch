package africa.credresearch.modules.project.domain.port;

import africa.credresearch.modules.project.domain.model.Activity;
import java.util.List;
import java.util.UUID;

public interface ActivityRepository {

    Activity record(Activity activity);

    List<Activity> findByProject(UUID projectId, int limit, int offset);
}
