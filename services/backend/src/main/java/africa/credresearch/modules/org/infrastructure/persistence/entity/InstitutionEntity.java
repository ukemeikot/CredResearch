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
@Table(name = "institutions")
public class InstitutionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String country;

    private String type;

    @Column(name = "is_personal_tenant", nullable = false)
    private boolean personalTenant;

    @Column(nullable = false)
    private String status = "active";

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
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public boolean isPersonalTenant() { return personalTenant; }
    public void setPersonalTenant(boolean v) { this.personalTenant = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
