package africa.credresearch.modules.review.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_requests")
public class ReviewRequestEntity {
    @Id private UUID id;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "document_section_id") private UUID documentSectionId;
    @Column(name = "requested_by", nullable = false) private UUID requestedBy;
    @Column(name = "reviewer_user_id") private UUID reviewerUserId;
    @Column(name = "reviewer_email") private String reviewerEmail;
    @Column(nullable = false) private String status = "PENDING";
    private String note;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "decided_at") private Instant decidedAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; } public void setDocumentId(UUID v) { documentId = v; }
    public UUID getDocumentSectionId() { return documentSectionId; } public void setDocumentSectionId(UUID v) { documentSectionId = v; }
    public UUID getRequestedBy() { return requestedBy; } public void setRequestedBy(UUID v) { requestedBy = v; }
    public UUID getReviewerUserId() { return reviewerUserId; } public void setReviewerUserId(UUID v) { reviewerUserId = v; }
    public String getReviewerEmail() { return reviewerEmail; } public void setReviewerEmail(String v) { reviewerEmail = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getNote() { return note; } public void setNote(String v) { note = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getDecidedAt() { return decidedAt; } public void setDecidedAt(Instant v) { decidedAt = v; }
}
