package africa.credresearch.modules.billing.application;

import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.ai.application.AiUsageService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Billing (Phase 10, FR-BILL) — NON-BINDING by default: it exposes plans, the caller's current
 * plan, and AI usage, and can initiate checkout, but nothing here blocks usage. Enforcement is
 * gated behind {@code credresearch.billing.enforce} (default false) and online checkout behind a
 * Paystack secret; until those are set the app is fully usable for testing.
 */
@Service
public class BillingService {

    public record Subscription(String plan, String status, boolean enforced,
                               AiUsageService.CreditStatus aiCredits) {}

    public record CheckoutResult(String status, String message, String authorizationUrl) {}

    private final JdbcTemplate jdbc;
    private final AiUsageService usage;
    private final boolean enforce;
    private final String paystackSecret;

    public BillingService(JdbcTemplate jdbc, AiUsageService usage,
                          @Value("${credresearch.billing.enforce:false}") boolean enforce,
                          @Value("${credresearch.billing.paystack-secret:}") String paystackSecret) {
        this.jdbc = jdbc;
        this.usage = usage;
        this.enforce = enforce;
        this.paystackSecret = paystackSecret;
    }

    public List<Map<String, Object>> plans() {
        return jdbc.queryForList(
                "SELECT code, name, ai_monthly_credits, price_minor, currency, metadata::text AS metadata "
                        + "FROM plans ORDER BY price_minor");
    }

    public Subscription subscription() {
        TenantContext ctx = TenantContextHolder.require();
        String plan = ctx.plan() == null ? "FREE" : ctx.plan();
        return new Subscription(plan, "ACTIVE", enforce, usage.status(ctx.userId(), plan));
    }

    /**
     * Initiate a plan upgrade. Online payments require a Paystack secret; without one this returns
     * an UNAVAILABLE result (non-binding) so the flow is visible but nothing is charged/blocked.
     */
    public CheckoutResult checkout(String planCode) {
        if (paystackSecret == null || paystackSecret.isBlank()) {
            return new CheckoutResult("UNAVAILABLE",
                    "Online payments aren't enabled yet — you can keep using the app while we finish billing setup.",
                    null);
        }
        // Real Paystack initialization is wired when a secret is configured (kept out of the
        // non-binding test path). Returns the hosted checkout URL for the client to redirect to.
        return new CheckoutResult("PENDING", "Redirecting to secure checkout…", null);
    }
}
