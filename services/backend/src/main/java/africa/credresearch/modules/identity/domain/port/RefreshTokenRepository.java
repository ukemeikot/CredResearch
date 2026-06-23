package africa.credresearch.modules.identity.domain.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    record ActiveToken(UUID id, UUID userId, Instant expiresAt) {}

    void store(UUID userId, String tokenHash, Instant expiresAt, String device, String userAgent);

    /** Returns the token only if it exists, is unexpired and not revoked. */
    Optional<ActiveToken> findActiveByHash(String tokenHash);

    void revokeById(UUID id);

    void revokeAllForUser(UUID userId);
}
