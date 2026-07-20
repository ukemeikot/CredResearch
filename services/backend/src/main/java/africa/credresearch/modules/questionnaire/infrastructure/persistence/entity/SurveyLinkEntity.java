package africa.credresearch.modules.questionnaire.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "survey_links")
public class SurveyLinkEntity {
    @Id private UUID id;
    @Column(name = "questionnaire_id", nullable = false) private UUID questionnaireId;
    @Column(name = "token_hash", nullable = false) private String tokenHash;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getQuestionnaireId() { return questionnaireId; } public void setQuestionnaireId(UUID v) { questionnaireId = v; }
    public String getTokenHash() { return tokenHash; } public void setTokenHash(String v) { tokenHash = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public Instant getExpiresAt() { return expiresAt; } public void setExpiresAt(Instant v) { expiresAt = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
