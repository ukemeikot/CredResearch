package africa.credresearch.common.audit;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    private String ip;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UuidV7.generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // getters/setters
    public UUID getId() { return id; }
    public void setInstitutionId(UUID v) { this.institutionId = v; }
    public void setActorUserId(UUID v) { this.actorUserId = v; }
    public void setAction(String v) { this.action = v; }
    public void setTargetType(String v) { this.targetType = v; }
    public void setTargetId(UUID v) { this.targetId = v; }
    public void setMetadata(String v) { this.metadata = v; }
    public void setIp(String v) { this.ip = v; }
}
