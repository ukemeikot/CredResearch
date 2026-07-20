package africa.credresearch.modules.billing.interfaces.rest;

import africa.credresearch.modules.billing.application.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Billing", description = "Plans, subscription status, and checkout (Phase 10, FR-BILL). "
        + "Non-binding: nothing is blocked until enforcement + a payment provider are configured.")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    public record CheckoutRequest(String planCode) {}

    @GetMapping("/plans")
    @Operation(summary = "List plans with pricing + features")
    public List<Map<String, Object>> plans() {
        return service.plans();
    }

    @GetMapping("/subscription")
    @Operation(summary = "The caller's current plan, AI usage, and whether enforcement is on")
    public BillingService.Subscription subscription() {
        return service.subscription();
    }

    @PostMapping("/checkout")
    @Operation(summary = "Initiate a plan upgrade (no-op UNAVAILABLE result until payments are enabled)")
    public BillingService.CheckoutResult checkout(@RequestBody CheckoutRequest req) {
        return service.checkout(req.planCode());
    }
}
