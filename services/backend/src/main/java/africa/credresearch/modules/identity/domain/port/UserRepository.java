package africa.credresearch.modules.identity.domain.port;

import africa.credresearch.modules.identity.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for user persistence. Implemented by an infrastructure adapter. */
public interface UserRepository {

    User create(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void updatePasswordHash(UUID userId, String passwordHash);

    void markEmailVerified(UUID userId);

    void updateStatus(UUID userId, String status);

    /** Reassigns the user to another institution (tenant) — used by institution onboarding. */
    void updateInstitution(UUID userId, UUID institutionId);

    void updateProfile(UUID userId, String fullName, String academicLevel, String fieldOfStudy, String orcid);

    /** Tenant-scoped listing — never returns users outside {@code institutionId} (FR-TEN-1). */
    List<User> findByInstitution(UUID institutionId, int limit, int offset);

    Optional<User> findByIdAndInstitution(UUID id, UUID institutionId);
}
