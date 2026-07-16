package africa.credresearch.modules.identity.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.identity.domain.port.RoleRepository;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.org.application.OrgProvisioning;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Institution onboarding (Phase 1, FR-ORG): a signed-in user creates a real institution and becomes
 * its INSTITUTION_ADMIN, moving out of their personal workspace. After this, the client must
 * refresh its tokens to pick up the new tenant + role (the access token embeds both).
 */
@Service
public class OnboardingService {

    private static final String INSTITUTION_ADMIN = "INSTITUTION_ADMIN";

    private final OrgProvisioning orgProvisioning;
    private final UserRepository users;
    private final RoleRepository roles;
    private final africa.credresearch.common.audit.AuditService audit;

    public OnboardingService(OrgProvisioning orgProvisioning, UserRepository users, RoleRepository roles,
                             africa.credresearch.common.audit.AuditService audit) {
        this.orgProvisioning = orgProvisioning;
        this.users = users;
        this.roles = roles;
        this.audit = audit;
    }

    /** Creates the institution, reassigns the caller into it, and grants INSTITUTION_ADMIN. */
    @Transactional
    public UUID createInstitution(String name, String country, String type) {
        TenantContext ctx = TenantContextHolder.require();
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("INVALID_NAME", "An institution name is required");
        }
        // One onboarding per account: a user who already administers an institution can't re-onboard
        // (also avoids a duplicate role-grant on the uq_user_role constraint).
        if (ctx.hasRole(INSTITUTION_ADMIN)) {
            throw ApiException.conflict("ALREADY_ONBOARDED", "You already administer an institution");
        }
        UUID institutionId = orgProvisioning.createInstitution(name.trim(), country, type);
        users.updateInstitution(ctx.userId(), institutionId);
        roles.findIdByCode(INSTITUTION_ADMIN)
                .ifPresent(roleId -> roles.assignRoleToUser(ctx.userId(), roleId, institutionId));
        audit.record("INSTITUTION_ONBOARDED", "institution", institutionId, institutionId, ctx.userId(), null, null);
        return institutionId;
    }
}
