package africa.credresearch.modules.review.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_comments")
public class ReviewCommentEntity {
    @Id private UUID id;
    @Column(name = "review_request_id", nullable = false) private UUID reviewRequestId;
    @Column(name = "author_user_id") private UUID authorUserId;
    @Column(name = "author_label") private String authorLabel;
    @Column(name = "anchor_start") private Integer anchorStart;
    @Column(name = "anchor_end") private Integer anchorEnd;
    private String quote;
    @Column(nullable = false) private String body;
    @Column(nullable = false) private boolean resolved = false;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getReviewRequestId() { return reviewRequestId; } public void setReviewRequestId(UUID v) { reviewRequestId = v; }
    public UUID getAuthorUserId() { return authorUserId; } public void setAuthorUserId(UUID v) { authorUserId = v; }
    public String getAuthorLabel() { return authorLabel; } public void setAuthorLabel(String v) { authorLabel = v; }
    public Integer getAnchorStart() { return anchorStart; } public void setAnchorStart(Integer v) { anchorStart = v; }
    public Integer getAnchorEnd() { return anchorEnd; } public void setAnchorEnd(Integer v) { anchorEnd = v; }
    public String getQuote() { return quote; } public void setQuote(String v) { quote = v; }
    public String getBody() { return body; } public void setBody(String v) { body = v; }
    public boolean isResolved() { return resolved; } public void setResolved(boolean v) { resolved = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
