package africa.credresearch.modules.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Domain view of a user. Framework-free; mapped to/from the JPA entity in infrastructure. */
public record User(
        UUID id,
        UUID institutionId,
        UUID departmentId,
        String email,
        String passwordHash,
        String fullName,
        Instant emailVerifiedAt,
        String academicLevel,
        String fieldOfStudy,
        String orcid,
        String status) {

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
}
