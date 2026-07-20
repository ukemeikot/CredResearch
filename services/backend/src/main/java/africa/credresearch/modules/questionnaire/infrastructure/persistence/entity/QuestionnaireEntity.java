package africa.credresearch.modules.questionnaire.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "questionnaires")
public class QuestionnaireEntity {
    @Id private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false) private String title;
    @Column(name = "consent_text") private String consentText;
    @Column(nullable = false) private String status = "DRAFT";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; } public void setProjectId(UUID v) { projectId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getConsentText() { return consentText; } public void setConsentText(String v) { consentText = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
