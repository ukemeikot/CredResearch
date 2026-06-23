package africa.credresearch.modules.identity.infrastructure.persistence.repository;

import africa.credresearch.modules.identity.infrastructure.persistence.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserEntity> findByIdAndInstitutionId(UUID id, UUID institutionId);

    List<UserEntity> findByInstitutionIdAndDeletedAtIsNull(UUID institutionId, Pageable pageable);

    @Modifying
    @Query("update UserEntity u set u.passwordHash = :hash where u.id = :id")
    void updatePasswordHash(@Param("id") UUID id, @Param("hash") String hash);

    @Modifying
    @Query("update UserEntity u set u.emailVerifiedAt = CURRENT_TIMESTAMP where u.id = :id")
    void markEmailVerified(@Param("id") UUID id);

    @Modifying
    @Query("update UserEntity u set u.status = :status where u.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    @Modifying
    @Query("update UserEntity u set u.fullName = :fullName, u.academicLevel = :academicLevel, "
            + "u.fieldOfStudy = :fieldOfStudy, u.orcid = :orcid where u.id = :id")
    void updateProfile(@Param("id") UUID id, @Param("fullName") String fullName,
                       @Param("academicLevel") String academicLevel,
                       @Param("fieldOfStudy") String fieldOfStudy, @Param("orcid") String orcid);
}
