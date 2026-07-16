package africa.credresearch.modules.project.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(nullable = false)
    private String title;

    private String level;

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(name = "abstract")
    private String abstractText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UuidV7.generate();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID v) { this.institutionId = v; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID v) { this.departmentId = v; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID v) { this.ownerUserId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { this.level = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String v) { this.abstractText = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID v) { this.updatedBy = v; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant v) { this.deletedAt = v; }
}
