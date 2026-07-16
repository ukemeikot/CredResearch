package africa.credresearch.modules.project.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
public class InvitationEntity {
    @Id private UUID id;
    @Column(name = "institution_id", nullable = false) private UUID institutionId;
    @Column(name = "project_id") private UUID projectId;
    @Column(nullable = false) private String email;
    @Column(name = "role_code", nullable = false) private String roleCode;
    @Column(name = "token_hash", nullable = false) private String tokenHash;
    @Column(nullable = false) private String status = "PENDING";
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "accepted_user_id") private UUID acceptedUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate void preU() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID v) { institutionId = v; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID v) { projectId = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String v) { roleCode = v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { tokenHash = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { expiresAt = v; }
    public UUID getAcceptedUserId() { return acceptedUserId; }
    public void setAcceptedUserId(UUID v) { acceptedUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedBy(UUID v) { createdBy = v; }
}
