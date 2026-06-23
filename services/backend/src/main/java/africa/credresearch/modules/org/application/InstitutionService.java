package africa.credresearch.modules.org.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.org.domain.model.Institution;
import africa.credresearch.modules.org.domain.port.InstitutionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionService {

    private final InstitutionRepository institutions;

    public InstitutionService(InstitutionRepository institutions) {
        this.institutions = institutions;
    }

    public Institution get(UUID id) {
        requireSameTenantOrPlatform(id);
        return institutions.findById(id)
                .orElseThrow(() -> ApiException.notFound("INSTITUTION_NOT_FOUND", "Institution not found"));
    }

    public Institution create(String name, String country, String type) {
        // Only PLATFORM_ADMIN reaches here (enforced at controller); creates a real institution.
        return institutions.create(new Institution(null, name, country, type, false, "active"));
    }

    public Institution update(UUID id, String name, String country, String type) {
        requireSameTenantOrPlatform(id);
        institutions.update(id, name, country, type);
        return institutions.findById(id).orElseThrow();
    }

    /** A non-platform admin may only touch their own institution (FR-TEN-1). */
    private void requireSameTenantOrPlatform(UUID id) {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.isPlatformAdmin() && !id.equals(ctx.institutionId())) {
            throw ApiException.forbidden("CROSS_TENANT_DENIED", "Resource belongs to another tenant");
        }
    }
}
