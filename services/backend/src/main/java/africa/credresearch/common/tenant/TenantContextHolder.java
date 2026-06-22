package africa.credresearch.common.tenant;

import java.util.Optional;

/** Request-scoped holder for the current {@link TenantContext} (set by the JWT filter). */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /** Returns the current context or throws if unauthenticated — use in tenant-scoped services. */
    public static TenantContext require() {
        TenantContext ctx = CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("No tenant context bound to the current request");
        }
        return ctx;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
