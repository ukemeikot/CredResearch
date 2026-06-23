package africa.credresearch.modules.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import africa.credresearch.common.audit.AuditService;
import africa.credresearch.common.config.CredResearchProperties;
import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.notification.NotificationPort;
import africa.credresearch.common.ratelimit.LoginThrottle;
import africa.credresearch.common.security.JwtService;
import africa.credresearch.modules.identity.application.dto.AuthTokens;
import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.AuthTokenRepository;
import africa.credresearch.modules.identity.domain.port.RefreshTokenRepository;
import africa.credresearch.modules.identity.domain.port.RoleRepository;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.org.application.OrgProvisioning;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private UserRepository users;
    private RoleRepository roles;
    private RefreshTokenRepository refreshTokens;
    private AuthTokenRepository authTokens;
    private org.springframework.security.crypto.password.PasswordEncoder encoder;
    private JwtService jwtService;
    private LoginThrottle throttle;
    private NotificationPort notifications;
    private OrgProvisioning orgProvisioning;
    private AuditService audit;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        roles = mock(RoleRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        authTokens = mock(AuthTokenRepository.class);
        encoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        throttle = mock(LoginThrottle.class);
        notifications = mock(NotificationPort.class);
        orgProvisioning = mock(OrgProvisioning.class);
        audit = mock(AuditService.class);
        CredResearchProperties props = new CredResearchProperties(
                new CredResearchProperties.Auth(null, null, Duration.ofMinutes(15), Duration.ofDays(30),
                        Duration.ofHours(24), "credresearch"),
                new CredResearchProperties.Throttle(5, Duration.ofMinutes(15)),
                new CredResearchProperties.App("http://localhost:3000"),
                new CredResearchProperties.Email("no-reply@test.local"));
        service = new AuthService(users, roles, refreshTokens, authTokens, encoder, jwtService,
                throttle, notifications, orgProvisioning, audit, props);
    }

    private User activeUser(String hash, String status) {
        return new User(UUID.randomUUID(), UUID.randomUUID(), null, "a@b.com",
                hash, "Ada", null, null, null, null, status);
    }

    // ── register ──────────────────────────────────────────────────────────────
    @Test
    void register_whenEmailExists_throwsConflict() {
        when(users.existsByEmail("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register("a@b.com", "password1", "Ada", "1.2.3.4"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("EMAIL_TAKEN");
        verify(users, never()).create(any());
    }

    @Test
    void register_happyPath_createsTenantUserRoleAndSendsVerification() {
        UUID instId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User created = new User(UUID.randomUUID(), instId, null, "a@b.com", "hash", "Ada",
                null, null, null, null, "active");
        when(users.existsByEmail("a@b.com")).thenReturn(false);
        when(orgProvisioning.createPersonalTenant("Ada")).thenReturn(instId);
        when(encoder.encode("password1")).thenReturn("hash");
        when(users.create(any())).thenReturn(created);
        when(roles.findIdByCode("STUDENT")).thenReturn(Optional.of(roleId));

        UUID result = service.register("a@b.com", "password1", "Ada", "1.2.3.4");

        assertThat(result).isEqualTo(created.id());
        verify(roles).assignRoleToUser(created.id(), roleId, instId);
        verify(authTokens).store(eq(created.id()), eq(AuthTokenRepository.Type.EMAIL_VERIFY),
                anyString(), any(Instant.class));
        verify(notifications).sendEmail(eq("a@b.com"), anyString(), anyString());
    }

    // ── login ─────────────────────────────────────────────────────────────────
    @Test
    void login_whenThrottled_throws429() {
        when(throttle.isBlocked("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> service.login("a@b.com", "x", null, null, "ip"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("LOGIN_THROTTLED");
    }

    @Test
    void login_invalidPassword_recordsFailureAndThrows401() {
        when(throttle.isBlocked("a@b.com")).thenReturn(false);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(activeUser("hash", "active")));
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("a@b.com", "wrong", null, null, "ip"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
        verify(throttle).recordFailure("a@b.com");
    }

    @Test
    void login_suspendedAccount_throwsForbidden() {
        when(throttle.isBlocked("a@b.com")).thenReturn(false);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(activeUser("hash", "suspended")));
        when(encoder.matches("password1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login("a@b.com", "password1", null, null, "ip"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("ACCOUNT_SUSPENDED");
    }

    @Test
    void login_success_issuesTokensAndResetsThrottle() {
        User user = activeUser("hash", "active");
        when(throttle.isBlocked("a@b.com")).thenReturn(false);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password1", "hash")).thenReturn(true);
        when(roles.findRoleCodesForUser(user.id())).thenReturn(Set.of("STUDENT"));
        when(jwtService.issueAccessToken(any())).thenReturn("access-token");

        AuthTokens tokens = service.login("a@b.com", "password1", "web", "agent", "ip");

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.user().roles()).contains("STUDENT");
        verify(throttle).reset("a@b.com");
        verify(refreshTokens).store(eq(user.id()), anyString(), any(Instant.class), eq("web"), eq("agent"));
    }

    // ── refresh ───────────────────────────────────────────────────────────────
    @Test
    void refresh_rotatesToken() {
        UUID oldId = UUID.randomUUID();
        User user = activeUser("hash", "active");
        when(refreshTokens.findActiveByHash(anyString()))
                .thenReturn(Optional.of(new RefreshTokenRepository.ActiveToken(
                        oldId, user.id(), Instant.now().plusSeconds(1000))));
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(roles.findRoleCodesForUser(user.id())).thenReturn(Set.of("STUDENT"));
        when(jwtService.issueAccessToken(any())).thenReturn("access-2");

        AuthTokens tokens = service.refresh("raw-refresh", "web", "agent");

        assertThat(tokens.accessToken()).isEqualTo("access-2");
        verify(refreshTokens).revokeById(oldId);
        verify(refreshTokens).store(eq(user.id()), anyString(), any(Instant.class), eq("web"), eq("agent"));
    }

    @Test
    void refresh_invalidToken_throws401() {
        when(refreshTokens.findActiveByHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("bad", null, null))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_REFRESH_TOKEN");
    }

    // ── reset password ──────────────────────────────────────────────────────────
    @Test
    void resetPassword_updatesHashAndRevokesAllSessions() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        when(authTokens.findValid(anyString(), eq(AuthTokenRepository.Type.PASSWORD_RESET)))
                .thenReturn(Optional.of(new AuthTokenRepository.ValidToken(tokenId, userId)));
        when(encoder.encode("newpassword")).thenReturn("new-hash");

        service.resetPassword("raw", "newpassword");

        verify(authTokens).markUsed(tokenId);
        verify(users).updatePasswordHash(userId, "new-hash");
        verify(refreshTokens).revokeAllForUser(userId);
    }
}
