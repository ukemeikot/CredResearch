package africa.credresearch.modules.identity.infrastructure.persistence;

import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.identity.infrastructure.persistence.entity.UserEntity;
import africa.credresearch.modules.identity.infrastructure.persistence.repository.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User create(User user) {
        UserEntity e = new UserEntity();
        e.setInstitutionId(user.institutionId());
        e.setDepartmentId(user.departmentId());
        e.setEmail(user.email());
        e.setPasswordHash(user.passwordHash());
        e.setFullName(user.fullName());
        e.setEmailVerifiedAt(user.emailVerifiedAt());
        e.setAcademicLevel(user.academicLevel());
        e.setFieldOfStudy(user.fieldOfStudy());
        e.setOrcid(user.orcid());
        e.setStatus(user.status() == null ? "active" : user.status());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updatePasswordHash(UUID userId, String passwordHash) {
        jpa.updatePasswordHash(userId, passwordHash);
    }

    @Override
    @Transactional
    public void markEmailVerified(UUID userId) {
        jpa.markEmailVerified(userId);
    }

    @Override
    @Transactional
    public void updateStatus(UUID userId, String status) {
        jpa.updateStatus(userId, status);
    }

    @Override
    @Transactional
    public void updateProfile(UUID userId, String fullName, String academicLevel,
                              String fieldOfStudy, String orcid) {
        jpa.updateProfile(userId, fullName, academicLevel, fieldOfStudy, orcid);
    }

    @Override
    public List<User> findByInstitution(UUID institutionId, int limit, int offset) {
        int page = limit <= 0 ? 0 : offset / limit;
        return jpa.findByInstitutionIdAndDeletedAtIsNull(institutionId, PageRequest.of(page, Math.max(1, limit)))
                .stream().map(UserRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<User> findByIdAndInstitution(UUID id, UUID institutionId) {
        return jpa.findByIdAndInstitutionId(id, institutionId).map(UserRepositoryAdapter::toDomain);
    }

    static User toDomain(UserEntity e) {
        return new User(
                e.getId(), e.getInstitutionId(), e.getDepartmentId(), e.getEmail(),
                e.getPasswordHash(), e.getFullName(), e.getEmailVerifiedAt(),
                e.getAcademicLevel(), e.getFieldOfStudy(), e.getOrcid(), e.getStatus());
    }
}
