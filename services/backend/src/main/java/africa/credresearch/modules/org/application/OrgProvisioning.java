package africa.credresearch.modules.org.application;

import java.util.UUID;

/**
 * In-process service interface other modules use to provision tenants — the clean seam
 * between modules (no module touches another's tables; see modules/ARCHITECTURE.md).
 */
public interface OrgProvisioning {

    /** Creates a synthetic personal tenant for an independent user (FR-ORG-4); returns its id. */
    UUID createPersonalTenant(String displayName);
}
