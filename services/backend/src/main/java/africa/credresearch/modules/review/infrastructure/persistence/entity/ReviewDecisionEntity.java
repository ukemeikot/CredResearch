package africa.credresearch.modules.review.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_decisions")
public class ReviewDecisionEntity {
    @Id private UUID id;
    @Column(name = "review_request_id", nullable = false) private UUID reviewRequestId;
    @Column(nullable = false) private String decision;
    private String summary;
    @Column(name = "decided_by") private UUID decidedBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getReviewRequestId() { return reviewRequestId; } public void setReviewRequestId(UUID v) { reviewRequestId = v; }
    public String getDecision() { return decision; } public void setDecision(String v) { decision = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { summary = v; }
    public UUID getDecidedBy() { return decidedBy; } public void setDecidedBy(UUID v) { decidedBy = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
