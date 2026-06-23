package africa.credresearch.modules.identity.infrastructure.persistence;

import africa.credresearch.modules.identity.domain.port.RefreshTokenRepository;
import africa.credresearch.modules.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import africa.credresearch.modules.identity.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void store(UUID userId, String tokenHash, Instant expiresAt, String device, String userAgent) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setUserId(userId);
        e.setTokenHash(tokenHash);
        e.setExpiresAt(expiresAt);
        e.setDevice(device);
        e.setUserAgent(userAgent);
        jpa.save(e);
    }

    @Override
    public Optional<ActiveToken> findActiveByHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash)
                .filter(t -> t.getRevokedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .map(t -> new ActiveToken(t.getId(), t.getUserId(), t.getExpiresAt()));
    }

    @Override
    @Transactional
    public void revokeById(UUID id) {
        jpa.revokeById(id, Instant.now());
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllForUser(userId, Instant.now());
    }
}
