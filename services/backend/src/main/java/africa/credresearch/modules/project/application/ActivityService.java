package africa.credresearch.modules.project.application;

import africa.credresearch.modules.project.domain.model.Activity;
import africa.credresearch.modules.project.domain.port.ActivityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Records and lists project activity-feed entries (FR-PROJ-6). */
@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activities;
    private final ObjectMapper objectMapper;

    public ActivityService(ActivityRepository activities, ObjectMapper objectMapper) {
        this.activities = activities;
        this.objectMapper = objectMapper;
    }

    /** Records an activity entry. {@code payload} is serialized to JSON (may be null/empty). */
    public Activity record(UUID projectId, UUID actorUserId, String type, Map<String, Object> payload) {
        return activities.record(new Activity(null, projectId, actorUserId, type, toJson(payload), null));
    }

    public java.util.List<Activity> list(UUID projectId, int limit, int offset) {
        return activities.findByProject(projectId, limit, offset);
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize activity payload", e);
            return null;
        }
    }
}
