package africa.credresearch.modules.identity.interfaces.rest;

import africa.credresearch.common.security.AppUserPrincipal;
import africa.credresearch.modules.identity.application.AuthService;
import africa.credresearch.modules.identity.application.dto.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
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
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank String fullName) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password, String device) {}

    public record RefreshRequest(@NotBlank String refreshToken, String device) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record VerifyEmailRequest(@NotBlank String token) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}

    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 128) String password) {}

    // ── Response DTOs ────────────────────────────────────────────────────────
    public record UserSummary(UUID id, Set<String> roles, UUID institutionId, String plan) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserSummary user) {
        static TokenResponse from(AuthTokens t) {
            return new TokenResponse(t.accessToken(), t.refreshToken(), t.expiresIn(),
                    new UserSummary(t.user().id(), t.user().roles(), t.user().institutionId(), t.user().plan()));
        }
    }

    // ── Endpoints ────────────────────────────────────────────────────────────
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        UUID id = authService.register(req.email(), req.password(), req.fullName(), clientIp(http));
        return Map.of("userId", id, "message", "Registered. Check your email to verify your account.");
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return TokenResponse.from(authService.login(
                req.email(), req.password(), req.device(), http.getHeader("User-Agent"), clientIp(http)));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        return TokenResponse.from(authService.refresh(req.refreshToken(), req.device(), http.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req.refreshToken());
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.logoutAll(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.token());
        return Map.of("message", "Email verified.");
    }

    @PostMapping("/password/forgot")
    public Map<String, String> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return Map.of("message", "If that email exists, a reset link has been sent.");
    }

    @PostMapping("/password/reset")
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
