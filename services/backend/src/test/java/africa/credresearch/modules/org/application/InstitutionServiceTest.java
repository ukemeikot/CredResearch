package africa.credresearch.modules.org.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.org.domain.model.Institution;
import africa.credresearch.modules.org.domain.port.InstitutionRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tenant-isolation tests (FR-TEN-1): a tenant cannot read another tenant's institution. */
class InstitutionServiceTest {

    private final InstitutionRepository repo = mock(InstitutionRepository.class);
    private final InstitutionService service = new InstitutionService(repo);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    private void asUser(UUID institutionId, String... roles) {
        TenantContextHolder.set(new TenantContext(UUID.randomUUID(), institutionId, Set.of(roles), "FREE"));
    }

    @Test
    void get_ownInstitution_returnsIt() {
        UUID inst = UUID.randomUUID();
        asUser(inst, "INSTITUTION_ADMIN");
        when(repo.findById(inst)).thenReturn(Optional.of(
                new Institution(inst, "Mine", "NG", "university", false, "active")));

        assertThat(service.get(inst).name()).isEqualTo("Mine");
    }

    @Test
    void get_otherTenant_isForbidden() {
        UUID mine = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        asUser(mine, "INSTITUTION_ADMIN");

        assertThatThrownBy(() -> service.get(other))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_TENANT_DENIED");
    }

    @Test
    void get_platformAdmin_canReadAnyTenant() {
        UUID mine = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        asUser(mine, "PLATFORM_ADMIN");
        when(repo.findById(other)).thenReturn(Optional.of(
                new Institution(other, "Theirs", "NG", "university", false, "active")));

        assertThat(service.get(other).name()).isEqualTo("Theirs");
    }
}
