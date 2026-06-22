package africa.credresearch.modules.org.infrastructure.persistence.entity;

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
@Table(name = "departments")
public class DepartmentEntity {

    @Id
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
}
