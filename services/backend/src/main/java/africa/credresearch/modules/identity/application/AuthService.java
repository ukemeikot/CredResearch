package africa.credresearch.modules.identity.application;

import africa.credresearch.common.config.CredResearchProperties;
import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.notification.NotificationPort;
import africa.credresearch.common.ratelimit.LoginThrottle;
import africa.credresearch.common.security.AppUserPrincipal;
import africa.credresearch.common.security.JwtService;
import africa.credresearch.common.util.TokenHasher;
import africa.credresearch.modules.identity.application.dto.AuthenticatedUser;
import africa.credresearch.modules.identity.application.dto.AuthTokens;
import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.AuthTokenRepository;
import africa.credresearch.modules.identity.domain.port.RefreshTokenRepository;
import africa.credresearch.modules.identity.domain.port.RoleRepository;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.org.application.OrgProvisioning;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates authentication: registration, login, token rotation, email verification, reset. */
@Service
public class AuthService {

    private static final String DEFAULT_PLAN = "FREE";
    private static final String STUDENT_ROLE = "STUDENT";

    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final AuthTokenRepository authTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle loginThrottle;
    private final NotificationPort notifications;
    private final OrgProvisioning orgProvisioning;
    private final africa.credresearch.common.audit.AuditService audit;
    private final CredResearchProperties props;

    public AuthService(UserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
                       AuthTokenRepository authTokens, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginThrottle loginThrottle, NotificationPort notifications, OrgProvisioning orgProvisioning,
                       africa.credresearch.common.audit.AuditService audit, CredResearchProperties props) {
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.authTokens = authTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginThrottle = loginThrottle;
        this.notifications = notifications;
        this.orgProvisioning = orgProvisioning;
        this.audit = audit;
        this.props = props;
    }

    @Transactional
    public UUID register(String email, String rawPassword, String fullName, String ip) {
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with that email already exists");
        }
        UUID institutionId = orgProvisioning.createPersonalTenant(fullName);
        User created = users.create(new User(null, institutionId, null, email,
                passwordEncoder.encode(rawPassword), fullName, null, null, null, null, "active"));

        roles.findIdByCode(STUDENT_ROLE).ifPresent(roleId ->
                roles.assignRoleToUser(created.id(), roleId, institutionId));

        issueAndSendEmailToken(created, AuthTokenRepository.Type.EMAIL_VERIFY,
                "Verify your CredResearch email", "/verify-email");

        audit.record("USER_REGISTERED", "user", created.id(), institutionId, created.id(), null, ip);
        return created.id();
    }

    @Transactional
    public AuthTokens login(String email, String rawPassword, String device, String userAgent, String ip) {
        if (loginThrottle.isBlocked(email)) {
            throw ApiException.tooManyRequests("LOGIN_THROTTLED",
                    "Too many failed attempts. Try again later.");
        }
        User user = users.findByEmail(email).orElse(null);
        if (user == null || user.passwordHash() == null
                || !passwordEncoder.matches(rawPassword, user.passwordHash())) {
            loginThrottle.recordFailure(email);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (!user.isActive()) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "This account is suspended");
        }
        loginThrottle.reset(email);
        audit.record("LOGIN_SUCCESS", "user", user.id(), user.institutionId(), user.id(), null, ip);
        return issueTokens(user, device, userAgent);
    }

    @Transactional
    public AuthTokens refresh(String refreshToken, String device, String userAgent) {
        String hash = TokenHasher.sha256(refreshToken);
        RefreshTokenRepository.ActiveToken active = refreshTokens.findActiveByHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or expired"));
        // Rotate: the presented token is single-use.
        refreshTokens.revokeById(active.id());
        User user = users.findById(active.userId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Unknown user"));
        if (!user.isActive()) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "This account is suspended");
        }
        return issueTokens(user, device, userAgent);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.findActiveByHash(TokenHasher.sha256(refreshToken))
                .ifPresent(t -> refreshTokens.revokeById(t.id()));
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokens.revokeAllForUser(userId);
    }

    @Transactional
    public void verifyEmail(String token) {
        AuthTokenRepository.ValidToken valid = authTokens
                .findValid(TokenHasher.sha256(token), AuthTokenRepository.Type.EMAIL_VERIFY)
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN",
                        "Verification link is invalid or expired"));
        authTokens.markUsed(valid.id());
        users.markEmailVerified(valid.userId());
        audit.record("EMAIL_VERIFIED", "user", valid.userId(), null, valid.userId(), null, null);
    }

    /** Always succeeds from the caller's view — never reveals whether the email exists. */
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmail(email).ifPresent(user -> {
            authTokens.invalidateAllForUser(user.id(), AuthTokenRepository.Type.PASSWORD_RESET);
            issueAndSendEmailToken(user, AuthTokenRepository.Type.PASSWORD_RESET,
                    "Reset your CredResearch password", "/reset-password");
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        AuthTokenRepository.ValidToken valid = authTokens
                .findValid(TokenHasher.sha256(token), AuthTokenRepository.Type.PASSWORD_RESET)
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN",
                        "Reset link is invalid or expired"));
        authTokens.markUsed(valid.id());
        users.updatePasswordHash(valid.userId(), passwordEncoder.encode(newPassword));
        // Force re-authentication everywhere after a password change.
        refreshTokens.revokeAllForUser(valid.userId());
        audit.record("PASSWORD_RESET", "user", valid.userId(), null, valid.userId(), null, null);
    }

    private AuthTokens issueTokens(User user, String device, String userAgent) {
        Set<String> roleCodes = roles.findRoleCodesForUser(user.id());
        AppUserPrincipal principal = new AppUserPrincipal(
                user.id(), user.institutionId(), roleCodes, DEFAULT_PLAN);
        String accessToken = jwtService.issueAccessToken(principal);

        String refreshRaw = TokenHasher.randomToken();
        Instant expiresAt = Instant.now().plus(props.auth().refreshTokenTtl());
        refreshTokens.store(user.id(), TokenHasher.sha256(refreshRaw), expiresAt, device, userAgent);

        long expiresIn = props.auth().accessTokenTtl().toSeconds();
        return new AuthTokens(accessToken, refreshRaw, expiresIn,
                new AuthenticatedUser(user.id(), roleCodes, user.institutionId(), DEFAULT_PLAN));
    }

    private void issueAndSendEmailToken(User user, AuthTokenRepository.Type type,
                                        String subject, String path) {
        String raw = TokenHasher.randomToken();
        Instant expiresAt = Instant.now().plus(props.auth().emailTokenTtl());
        authTokens.store(user.id(), type, TokenHasher.sha256(raw), expiresAt);
        String link = props.app().baseUrl() + path + "?token=" + raw;
        notifications.sendEmail(user.email(), subject,
                "Hello " + (user.fullName() == null ? "" : user.fullName())
                        + ",\n\nUse this link: " + link + "\n\nThis link expires soon.");
    }
}
