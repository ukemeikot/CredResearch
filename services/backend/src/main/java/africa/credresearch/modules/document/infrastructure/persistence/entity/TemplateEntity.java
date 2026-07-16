package africa.credresearch.modules.document.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "templates")
public class TemplateEntity {
    @Id private UUID id;
    @Column(name = "institution_id") private UUID institutionId;
    @Column(name = "department_id") private UUID departmentId;
    @Column(nullable = false) private String name;
    private String level;
    @Column(name = "is_global", nullable = false) private boolean global;
    @Column(name = "citation_style", nullable = false) private String citationStyle = "APA";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "deleted_at") private Instant deletedAt;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate void preU() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID v) { institutionId = v; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID v) { departmentId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { level = v; }
    public boolean isGlobal() { return global; }
    public void setGlobal(boolean v) { global = v; }
    public String getCitationStyle() { return citationStyle; }
    public void setCitationStyle(String v) { citationStyle = v; }
    public void setCreatedBy(UUID v) { createdBy = v; }
    public void setUpdatedBy(UUID v) { updatedBy = v; }
    public Instant getDeletedAt() { return deletedAt; }
}
