package africa.credresearch.modules.identity.interfaces.rest;

import africa.credresearch.common.security.AppUserPrincipal;
import africa.credresearch.common.security.AuthCookies;
import africa.credresearch.common.security.JwtService;
import africa.credresearch.modules.identity.application.AuthService;
import africa.credresearch.modules.identity.application.dto.AuthTokens;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, token lifecycle, email "
        + "verification, and password reset. All endpoints are public (no bearer token required).")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies cookies;
    private final JwtService jwtService;

    public AuthController(AuthService authService, AuthCookies cookies, JwtService jwtService) {
        this.authService = authService;
        this.cookies = cookies;
        this.jwtService = jwtService;
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────
    public record RegisterRequest(
            @Schema(description = "Account email (case-insensitive, unique)", example = "ada@example.com")
            @Email @NotBlank String email,
            @Schema(description = "Password, 8–128 chars", example = "password123", minLength = 8, maxLength = 128)
            @NotBlank @Size(min = 8, max = 128) String password,
            @Schema(description = "Display name", example = "Ada Lovelace")
            @NotBlank String fullName) {}

    public record LoginRequest(
            @Schema(example = "ada@example.com") @Email @NotBlank String email,
            @Schema(example = "password123") @NotBlank String password,
            @Schema(description = "Optional device label recorded with the session", example = "web")
            String device) {}

    public record RefreshRequest(
            @Schema(description = "The opaque refresh token from login. Optional — the browser "
                    + "sends it automatically via the HttpOnly cr_refresh cookie.") String refreshToken,
            String device) {}

    public record LogoutRequest(
            @Schema(description = "Refresh token to revoke. Optional — read from the cr_refresh "
                    + "cookie when absent.") String refreshToken) {}

    public record VerifyEmailRequest(
            @Schema(description = "Token from the verification email link") @NotBlank String token) {}

    public record ForgotPasswordRequest(@Schema(example = "ada@example.com") @Email @NotBlank String email) {}

    public record ResetPasswordRequest(
            @Schema(description = "Token from the reset email link") @NotBlank String token,
            @Schema(description = "New password, 8–128 chars") @NotBlank @Size(min = 8, max = 128) String password) {}

    // ── Response DTOs ────────────────────────────────────────────────────────
    public record UserSummary(UUID id, Set<String> roles, UUID institutionId, String plan) {}

    public record TokenResponse(
            @Schema(description = "RS256 JWT access token (15 min TTL)") String accessToken,
            @Schema(description = "Opaque refresh token (30 days, single-use/rotated)") String refreshToken,
            @Schema(description = "Access-token lifetime in seconds", example = "900") long expiresIn,
            UserSummary user) {
        static TokenResponse from(AuthTokens t) {
            return new TokenResponse(t.accessToken(), t.refreshToken(), t.expiresIn(),
                    new UserSummary(t.user().id(), t.user().roles(), t.user().institutionId(), t.user().plan()));
        }
    }

    // ── Endpoints ────────────────────────────────────────────────────────────
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a new account",
            description = "Creates a user in a synthetic personal tenant, assigns the STUDENT role, "
                    + "and emails a verification link.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created; verification email sent"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @io.swagger.v3.oas.annotations.media.Content()),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        UUID id = authService.register(req.email(), req.password(), req.fullName(), clientIp(http));
        return Map.of("userId", id, "message", "Registered. Check your email to verify your account.");
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in", description = "Returns an access token + a rotating refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @io.swagger.v3.oas.annotations.media.Content()),
            @ApiResponse(responseCode = "403", description = "Account suspended", content = @io.swagger.v3.oas.annotations.media.Content()),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts (throttled)", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        AuthTokens tokens = authService.login(
                req.email(), req.password(), req.device(), http.getHeader("User-Agent"), clientIp(http));
        var builder = ResponseEntity.ok();
        cookies.write(builder, tokens.accessToken(), tokens.refreshToken());
        return builder.body(TokenResponse.from(tokens));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotate tokens",
            description = "Exchanges a valid refresh token for a new access + refresh token. The "
                    + "presented refresh token is single-use and is revoked on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or already used", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest req, HttpServletRequest http) {
        // Cookie first (browser session), then request body (API clients / legacy).
        String presented = AuthCookies.read(http, AuthCookies.REFRESH);
        if (presented == null && req != null) {
            presented = req.refreshToken();
        }
        if (presented == null || presented.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String device = req != null ? req.device() : null;
        AuthTokens tokens = authService.refresh(presented, device, http.getHeader("User-Agent"));
        var builder = ResponseEntity.ok();
        cookies.write(builder, tokens.accessToken(), tokens.refreshToken());
        return builder.body(TokenResponse.from(tokens));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Log out (revoke one refresh token)", description = "Idempotent.")
    @ApiResponse(responseCode = "204", description = "Session revoked (or already invalid)")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest req,
                                       HttpServletRequest http) {
        String presented = AuthCookies.read(http, AuthCookies.REFRESH);
        if (presented == null && req != null) {
            presented = req.refreshToken();
        }
        if (presented != null && !presented.isBlank()) {
            authService.logout(presented);
        }
        var builder = ResponseEntity.noContent();
        cookies.clear(builder);
        return builder.build();
    }

    @org.springframework.web.bind.annotation.GetMapping("/session-token")
    @Operation(summary = "Mint a short-lived access token for the current session",
            description = "For clients that cannot send the HttpOnly cookie themselves — notably the "
                    + "Yjs collaboration WebSocket handshake. Authenticated via the cr_access cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fresh access token issued"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public ResponseEntity<Map<String, Object>> sessionToken(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("token", jwtService.issueAccessToken(principal)));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Log out all sessions",
            description = "Revokes every refresh token for the authenticated user. Requires a bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All sessions revoked"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.logoutAll(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @SecurityRequirements
    @Operation(summary = "Verify email", description = "Consumes a single-use verification token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified"),
            @ApiResponse(responseCode = "400", description = "Token invalid or expired", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public Map<String, String> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.token());
        return Map.of("message", "Email verified.");
    }

    @PostMapping("/password/forgot")
    @SecurityRequirements
    @Operation(summary = "Request a password reset",
            description = "Always returns 200 and never reveals whether the email exists.")
    @ApiResponse(responseCode = "200", description = "Reset email sent if the account exists")
    public Map<String, String> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return Map.of("message", "If that email exists, a reset link has been sent.");
    }

    @PostMapping("/password/reset")
    @SecurityRequirements
    @Operation(summary = "Reset password",
            description = "Consumes a single-use reset token, sets a new password, and revokes all sessions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Token invalid or expired", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.password());
        return Map.of("message", "Password updated. Please log in again.");
    }

    private static String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
