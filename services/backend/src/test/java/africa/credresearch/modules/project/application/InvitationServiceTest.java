package africa.credresearch.modules.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import africa.credresearch.common.config.CredResearchProperties;
import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.notification.NotificationPort;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.project.domain.InvitationStatus;
import africa.credresearch.modules.project.domain.model.Invitation;
import africa.credresearch.modules.project.domain.port.InvitationRepository;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Invitation acceptance rules (FR-PROJ-3): email match, same-tenant, idempotency. */
class InvitationServiceTest {

    private final InvitationRepository invitations = mock(InvitationRepository.class);
    private final ProjectMemberRepository members = mock(ProjectMemberRepository.class);
    private final ProjectAccessGuard accessGuard = mock(ProjectAccessGuard.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationPort notifications = mock(NotificationPort.class);
    private final CredResearchProperties props = mock(CredResearchProperties.class);

    private final InvitationService service = new InvitationService(
            invitations, members, accessGuard, activityService, users, notifications, props);

    private final UUID institution = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    private void caller(String email, UUID inst) {
        TenantContextHolder.set(new TenantContext(callerId, inst, Set.of("STUDENT"), "FREE"));
        when(users.findById(callerId)).thenReturn(Optional.of(new User(
                callerId, inst, null, email, "hash", "Ada", null, null, null, null, "active")));
    }

    private Invitation pending(String email, UUID inst) {
        return new Invitation(UUID.randomUUID(), inst, projectId, email, "SUPERVISOR",
                InvitationStatus.PENDING, Instant.now().plusSeconds(3600), null, Instant.now());
    }

    @Test
    void acceptAddsMemberAndMarksAccepted() {
        caller("ada@example.com", institution);
        when(invitations.findByTokenHash(any())).thenReturn(Optional.of(pending("ada@example.com", institution)));
        when(members.existsByProjectAndUser(projectId, callerId)).thenReturn(false);

        UUID result = service.accept("raw-token");

        assertThat(result).isEqualTo(projectId);
        verify(members).add(any());
        verify(invitations).markAccepted(any(), org.mockito.ArgumentMatchers.eq(callerId));
    }

    @Test
    void acceptRejectsEmailMismatch() {
        caller("someone.else@example.com", institution);
        when(invitations.findByTokenHash(any())).thenReturn(Optional.of(pending("ada@example.com", institution)));

        assertThatThrownBy(() -> service.accept("raw-token")).isInstanceOf(ApiException.class);
        verify(members, never()).add(any());
    }

    @Test
    void acceptRejectsCrossTenant() {
        caller("ada@example.com", UUID.randomUUID()); // caller in a different institution
        when(invitations.findByTokenHash(any())).thenReturn(Optional.of(pending("ada@example.com", institution)));

        assertThatThrownBy(() -> service.accept("raw-token")).isInstanceOf(ApiException.class);
        verify(members, never()).add(any());
    }
}
