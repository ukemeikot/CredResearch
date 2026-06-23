package africa.credresearch.modules.identity.infrastructure.persistence;

import africa.credresearch.modules.identity.domain.port.AuthTokenRepository;
import africa.credresearch.modules.identity.infrastructure.persistence.entity.AuthTokenEntity;
import africa.credresearch.modules.identity.infrastructure.persistence.repository.AuthTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthTokenRepositoryAdapter implements AuthTokenRepository {

    private final AuthTokenJpaRepository jpa;

    public AuthTokenRepositoryAdapter(AuthTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void store(UUID userId, Type type, String tokenHash, Instant expiresAt) {
        AuthTokenEntity e = new AuthTokenEntity();
        e.setUserId(userId);
        e.setType(type.name());
        e.setTokenHash(tokenHash);
        e.setExpiresAt(expiresAt);
        jpa.save(e);
    }

    @Override
    public Optional<ValidToken> findValid(String tokenHash, Type type) {
        return jpa.findByTokenHashAndType(tokenHash, type.name())
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .map(t -> new ValidToken(t.getId(), t.getUserId()));
    }

    @Override
    @Transactional
    public void markUsed(UUID id) {
        jpa.markUsed(id, Instant.now());
    }

    @Override
    @Transactional
    public void invalidateAllForUser(UUID userId, Type type) {
        jpa.invalidateAllForUser(userId, type.name(), Instant.now());
    }
}
