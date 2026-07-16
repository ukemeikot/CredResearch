package africa.credresearch.modules.document.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_sections")
public class DocumentSectionEntity {
    @Id private UUID id;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "order_index", nullable = false) private int orderIndex;
    private String chapter;
    @Column(nullable = false) private String heading;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String content;
    @Column(name = "content_text") private String contentText;
    @Column(nullable = false) private int version = 1;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by") private UUID updatedBy;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate void preU() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID v) { id = v; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID v) { documentId = v; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int v) { orderIndex = v; }
    public String getChapter() { return chapter; }
    public void setChapter(String v) { chapter = v; }
    public String getHeading() { return heading; }
    public void setHeading(String v) { heading = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getContentText() { return contentText; }
    public void setContentText(String v) { contentText = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { version = v; }
    public void setUpdatedBy(UUID v) { updatedBy = v; }
}
