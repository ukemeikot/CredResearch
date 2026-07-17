package africa.credresearch.modules.ai.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_requests")
public class AiRequestEntity {
    @Id private UUID id;
    @Column(name = "institution_id") private UUID institutionId;
    @Column(name = "project_id") private UUID projectId;
    @Column(name = "document_id") private UUID documentId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "feature_key", nullable = false) private String featureKey;
    private String model;
    @Column(nullable = false) private String status = "OK";
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setInstitutionId(UUID v) { institutionId = v; }
    public void setProjectId(UUID v) { projectId = v; }
    public void setDocumentId(UUID v) { documentId = v; }
    public void setUserId(UUID v) { userId = v; }
    public void setFeatureKey(String v) { featureKey = v; }
    public void setModel(String v) { model = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
}
