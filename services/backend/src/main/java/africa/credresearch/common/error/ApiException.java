package africa.credresearch.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base for expected, client-facing errors. Carries an HTTP status, a stable machine
 * {@code code}, and a human title/detail rendered as RFC 7807 problem+json.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    public ApiException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public static ApiException notFound(String code, String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, code, "Resource not found", detail);
    }

    public static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, "Conflict", detail);
    }

    public static ApiException unauthorized(String code, String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, "Authentication failed", detail);
    }

    public static ApiException forbidden(String code, String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "Forbidden", detail);
    }

    public static ApiException badRequest(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, "Invalid request", detail);
    }

    public static ApiException tooManyRequests(String code, String detail) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, code, "Too many requests", detail);
    }

    public static ApiException serviceUnavailable(String code, String detail) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, "Service unavailable", detail);
    }

    /** Generic factory for statuses without a dedicated helper (e.g. 402 Payment Required). */
    public static ApiException of(HttpStatus status, String code, String detail) {
        return new ApiException(status, code, status.getReasonPhrase(), detail);
    }
}
