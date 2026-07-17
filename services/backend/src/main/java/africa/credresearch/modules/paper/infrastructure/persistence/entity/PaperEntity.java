package africa.credresearch.modules.paper.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "papers")
public class PaperEntity {
    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    private String filename;
    private String title;
    private String authors;
    private Integer year;
    private String doi;
    private String journal;

    @Column(name = "extraction_status", nullable = false)
    private String extractionStatus = "PENDING";

    @Column(name = "text_content")
    private String textContent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (id == null) id = UuidV7.generate();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID v) { projectId = v; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID v) { uploadedBy = v; }
    public String getFilename() { return filename; }
    public void setFilename(String v) { filename = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getAuthors() { return authors; }
    public void setAuthors(String v) { authors = v; }
    public Integer getYear() { return year; }
    public void setYear(Integer v) { year = v; }
    public String getDoi() { return doi; }
    public void setDoi(String v) { doi = v; }
    public String getJournal() { return journal; }
    public void setJournal(String v) { journal = v; }
    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String v) { extractionStatus = v; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String v) { textContent = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
}
