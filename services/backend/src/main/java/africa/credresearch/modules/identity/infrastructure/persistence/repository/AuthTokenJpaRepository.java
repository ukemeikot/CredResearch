package africa.credresearch.modules.identity.infrastructure.persistence.repository;

import africa.credresearch.modules.identity.infrastructure.persistence.entity.AuthTokenEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenJpaRepository extends JpaRepository<AuthTokenEntity, UUID> {

    Optional<AuthTokenEntity> findByTokenHashAndType(String tokenHash, String type);

    @Modifying
    @Query("update AuthTokenEntity t set t.usedAt = :now where t.id = :id")
    void markUsed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("update AuthTokenEntity t set t.usedAt = :now "
            + "where t.userId = :userId and t.type = :type and t.usedAt is null")
    void invalidateAllForUser(@Param("userId") UUID userId, @Param("type") String type,
                              @Param("now") Instant now);
}
