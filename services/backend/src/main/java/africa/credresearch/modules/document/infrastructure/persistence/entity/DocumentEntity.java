package africa.credresearch.modules.document.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(name = "template_id", nullable = false) private UUID templateId;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String status = "DRAFT";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "deleted_at") private Instant deletedAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate void preU() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID v) { projectId = v; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID v) { templateId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public void setCreatedBy(UUID v) { createdBy = v; }
    public void setUpdatedBy(UUID v) { updatedBy = v; }
    public Instant getDeletedAt() { return deletedAt; }
}
