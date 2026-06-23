package africa.credresearch.modules.identity.infrastructure.persistence.entity;

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
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "academic_level")
    private String academicLevel;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    private String orcid;

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID v) { this.institutionId = v; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID v) { this.departmentId = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant v) { this.emailVerifiedAt = v; }
    public String getAcademicLevel() { return academicLevel; }
    public void setAcademicLevel(String v) { this.academicLevel = v; }
    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String v) { this.fieldOfStudy = v; }
    public String getOrcid() { return orcid; }
    public void setOrcid(String v) { this.orcid = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
