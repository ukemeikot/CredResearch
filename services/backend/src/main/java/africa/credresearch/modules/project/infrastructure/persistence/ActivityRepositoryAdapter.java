package africa.credresearch.modules.project.infrastructure.persistence;

import africa.credresearch.modules.project.domain.model.Activity;
import africa.credresearch.modules.project.domain.port.ActivityRepository;
import africa.credresearch.modules.project.infrastructure.persistence.entity.ActivityEntity;
import africa.credresearch.modules.project.infrastructure.persistence.repository.ActivityJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ActivityRepositoryAdapter implements ActivityRepository {

    private final ActivityJpaRepository jpa;

    public ActivityRepositoryAdapter(ActivityJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Activity record(Activity activity) {
        ActivityEntity e = new ActivityEntity();
        e.setProjectId(activity.projectId());
        e.setActorUserId(activity.actorUserId());
        e.setType(activity.type());
        e.setPayload(activity.payload());
        return toDomain(jpa.save(e));
    }

    @Override
    public List<Activity> findByProject(UUID projectId, int limit, int offset) {
        int size = Math.max(1, limit);
        int page = limit <= 0 ? 0 : offset / size;
        return jpa.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(Math.max(0, page), size))
                .stream().map(ActivityRepositoryAdapter::toDomain).toList();
    }

    static Activity toDomain(ActivityEntity e) {
        return new Activity(e.getId(), e.getProjectId(), e.getActorUserId(), e.getType(),
                e.getPayload(), e.getCreatedAt());
    }
}
