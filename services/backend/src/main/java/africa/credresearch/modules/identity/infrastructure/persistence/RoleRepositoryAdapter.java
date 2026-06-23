package africa.credresearch.modules.identity.infrastructure.persistence;

import africa.credresearch.modules.identity.domain.port.RoleRepository;
import africa.credresearch.modules.identity.infrastructure.persistence.entity.RoleEntity;
import africa.credresearch.modules.identity.infrastructure.persistence.entity.UserRoleEntity;
import africa.credresearch.modules.identity.infrastructure.persistence.repository.RoleJpaRepository;
import africa.credresearch.modules.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository roleJpa;
    private final UserRoleJpaRepository userRoleJpa;

    public RoleRepositoryAdapter(RoleJpaRepository roleJpa, UserRoleJpaRepository userRoleJpa) {
        this.roleJpa = roleJpa;
        this.userRoleJpa = userRoleJpa;
    }

    @Override
    public Optional<UUID> findIdByCode(String code) {
        return roleJpa.findByCode(code).map(RoleEntity::getId);
    }

    @Override
    @Transactional
    public void assignRoleToUser(UUID userId, UUID roleId, UUID institutionId) {
        UserRoleEntity e = new UserRoleEntity();
        e.setUserId(userId);
        e.setRoleId(roleId);
        e.setInstitutionId(institutionId);
        userRoleJpa.save(e);
    }

    @Override
    public Set<String> findRoleCodesForUser(UUID userId) {
        return userRoleJpa.findRolesForUser(userId).stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }
}
