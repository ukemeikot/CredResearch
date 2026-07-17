package africa.credresearch.modules.ai.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_responses")
public class AiResponseEntity {
    @Id private UUID id;
    @Column(name = "ai_request_id", nullable = false) private UUID aiRequestId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "output_json", columnDefinition = "jsonb") private String outputJson;
    @Column(name = "finish_reason") private String finishReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public void setAiRequestId(UUID v) { aiRequestId = v; }
    public void setOutputJson(String v) { outputJson = v; }
    public void setFinishReason(String v) { finishReason = v; }
}
