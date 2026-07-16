package africa.credresearch.modules.document.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_versions")
public class DocumentVersionEntity {
    @Id private UUID id;
    @Column(name = "document_section_id", nullable = false) private UUID documentSectionId;
    @Column(nullable = false) private int version;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String content;
    @Column(name = "content_text") private String contentText;
    @Column(name = "authored_by") private UUID authoredBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getDocumentSectionId() { return documentSectionId; }
    public void setDocumentSectionId(UUID v) { documentSectionId = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { version = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getContentText() { return contentText; }
    public void setContentText(String v) { contentText = v; }
    public UUID getAuthoredBy() { return authoredBy; }
    public void setAuthoredBy(UUID v) { authoredBy = v; }
    public Instant getCreatedAt() { return createdAt; }
}
