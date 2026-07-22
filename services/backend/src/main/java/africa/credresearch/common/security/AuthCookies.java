package africa.credresearch.common.security;

import africa.credresearch.common.config.CredResearchProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Builds/reads the auth cookies that back the browser session. Tokens live in {@code HttpOnly}
 * cookies (unreadable by JS → resistant to XSS token theft) rather than {@code localStorage}.
 *
 * <ul>
 *   <li>{@code cr_access}  — the RS256 access token, {@code Path=/} so every API call carries it.</li>
 *   <li>{@code cr_refresh} — the opaque refresh token, {@code Path=/api/v1/auth} so it is only ever
 *       sent to the auth endpoints (refresh/logout), never leaked to the rest of the API.</li>
 * </ul>
 *
 * Both are {@code SameSite=Lax} — safe against cross-site POST CSRF while still working for the
 * same-origin SPA (the LB serves the web app and {@code /api} on one host). {@code Secure} is on by
 * default and only disabled for local HTTP dev via {@code credresearch.auth.cookie-secure=false}.
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "cr_access";
    public static final String REFRESH = "cr_refresh";
    private static final String REFRESH_PATH = "/api/v1/auth";

    private final CredResearchProperties props;
    private final boolean secure;

    public AuthCookies(CredResearchProperties props,
                       @Value("${credresearch.auth.cookie-secure:true}") boolean secure) {
        this.props = props;
        this.secure = secure;
    }

    /** Attach fresh access + refresh cookies to the response. */
    public void write(ResponseEntity.HeadersBuilder<?> builder, String accessToken, String refreshToken) {
        builder.header(HttpHeaders.SET_COOKIE, accessCookie(accessToken).toString());
        builder.header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString());
    }

    /** Attach expired (empty) cookies so the browser drops the session. */
    public void clear(ResponseEntity.HeadersBuilder<?> builder) {
        builder.header(HttpHeaders.SET_COOKIE, expire(ACCESS, "/").toString());
        builder.header(HttpHeaders.SET_COOKIE, expire(REFRESH, REFRESH_PATH).toString());
    }

    private ResponseCookie accessCookie(String value) {
        return base(ACCESS, value, "/", props.auth().accessTokenTtl().getSeconds());
    }

    private ResponseCookie refreshCookie(String value) {
        return base(REFRESH, value, REFRESH_PATH, props.auth().refreshTokenTtl().getSeconds());
    }

    private ResponseCookie base(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie expire(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(0)
                .build();
    }

    /** Read a named cookie's value from the request, or {@code null} if absent. */
    public static String read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var c : request.getCookies()) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }
}
