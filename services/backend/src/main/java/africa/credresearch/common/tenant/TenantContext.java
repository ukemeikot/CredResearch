package africa.credresearch.common.tenant;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated caller's tenant + identity, derived from the access token on every request.
 * Tenant-scoped queries MUST filter by {@link #institutionId()} (FR-TEN-1). No endpoint accepts a
 * client-supplied institution id to widen scope.
 */
public record TenantContext(UUID userId, UUID institutionId, Set<String> roles, String plan) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isPlatformAdmin() {
        return hasRole("PLATFORM_ADMIN");
    }
}
