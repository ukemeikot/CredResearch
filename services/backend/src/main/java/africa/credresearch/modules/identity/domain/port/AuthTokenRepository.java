package africa.credresearch.modules.identity.domain.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Single-use email-verification and password-reset tokens (stored hashed). */
public interface AuthTokenRepository {

    enum Type { EMAIL_VERIFY, PASSWORD_RESET }

    record ValidToken(UUID id, UUID userId) {}

    void store(UUID userId, Type type, String tokenHash, Instant expiresAt);

    /** Returns the token only if unexpired and unused, matching the given type. */
    Optional<ValidToken> findValid(String tokenHash, Type type);

    void markUsed(UUID id);

    void invalidateAllForUser(UUID userId, Type type);
}
