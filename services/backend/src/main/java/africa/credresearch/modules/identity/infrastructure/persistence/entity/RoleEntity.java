package africa.credresearch.modules.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    public UUID getId() { return id; }
    public String getCode() { return code; }
}
