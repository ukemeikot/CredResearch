package africa.credresearch.modules.org.application;

import africa.credresearch.modules.org.domain.model.Institution;
import africa.credresearch.modules.org.domain.port.InstitutionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrgProvisioningService implements OrgProvisioning {

    private final InstitutionRepository institutions;

    public OrgProvisioningService(InstitutionRepository institutions) {
        this.institutions = institutions;
    }

    @Override
    public UUID createPersonalTenant(String displayName) {
        String name = (displayName == null || displayName.isBlank() ? "Personal" : displayName) + " — workspace";
        Institution created = institutions.create(
                new Institution(null, name, null, "personal", true, "active"));
        return created.id();
    }

    @Override
    public UUID createInstitution(String name, String country, String type) {
        Institution created = institutions.create(new Institution(null, name, country,
                type == null || type.isBlank() ? "university" : type, false, "active"));
        return created.id();
    }
}
