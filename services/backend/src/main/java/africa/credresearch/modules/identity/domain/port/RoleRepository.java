package africa.credresearch.modules.identity.domain.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository {

    Optional<UUID> findIdByCode(String code);

    void assignRoleToUser(UUID userId, UUID roleId, UUID institutionId);

    Set<String> findRoleCodesForUser(UUID userId);
}
