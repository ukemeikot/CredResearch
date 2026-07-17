package africa.credresearch.modules.disclosure.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_disclosure_entries")
public class DisclosureEntryEntity {
    @Id private UUID id;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "document_section_id") private UUID documentSectionId;
    @Column(name = "ai_request_id") private UUID aiRequestId;
    @Column(name = "user_id") private UUID userId;
    @Column(name = "feature_key") private String featureKey;
    private String model;
    @Column(name = "suggestion_summary") private String suggestionSummary;
    @Column(nullable = false) private String action = "accepted";
    @Column(name = "prev_hash") private String prevHash;
    @Column(name = "entry_hash", nullable = false) private String entryHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getDocumentSectionId() { return documentSectionId; }
    public void setDocumentSectionId(UUID v) { documentSectionId = v; }
    public void setDocumentId(UUID v) { documentId = v; }
    public void setAiRequestId(UUID v) { aiRequestId = v; }
    public void setUserId(UUID v) { userId = v; }
    public String getFeatureKey() { return featureKey; }
    public void setFeatureKey(String v) { featureKey = v; }
    public String getModel() { return model; }
    public void setModel(String v) { model = v; }
    public String getSuggestionSummary() { return suggestionSummary; }
    public void setSuggestionSummary(String v) { suggestionSummary = v; }
    public String getAction() { return action; }
    public void setAction(String v) { action = v; }
    public void setPrevHash(String v) { prevHash = v; }
    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String v) { entryHash = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
}
