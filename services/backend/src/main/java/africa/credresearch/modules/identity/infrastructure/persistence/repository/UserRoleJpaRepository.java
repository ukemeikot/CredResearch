package africa.credresearch.modules.identity.infrastructure.persistence.repository;

import africa.credresearch.modules.identity.infrastructure.persistence.entity.RoleEntity;
import africa.credresearch.modules.identity.infrastructure.persistence.entity.UserRoleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UUID> {

    @Query("select r from RoleEntity r, UserRoleEntity ur "
            + "where ur.roleId = r.id and ur.userId = :userId")
    List<RoleEntity> findRolesForUser(@Param("userId") UUID userId);
}
