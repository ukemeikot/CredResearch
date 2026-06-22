package africa.credresearch.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Writes security/admin-significant events to {@code audit_logs} (Security spec §7, FR-X-4). */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String action, String targetType, UUID targetId,
                       UUID institutionId, UUID actorUserId, Map<String, Object> metadata, String ip) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setInstitutionId(institutionId);
        entity.setActorUserId(actorUserId);
        entity.setIp(ip);
        entity.setMetadata(toJson(metadata));
        repository.save(entity);
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit metadata", e);
            return null;
        }
    }
}
