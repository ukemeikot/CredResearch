package africa.credresearch.modules.ai.interfaces.rest;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.ai.application.AiUsageService;
import africa.credresearch.modules.ai.infrastructure.AiWorkerClient;
import africa.credresearch.modules.identity.application.ProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI Research Assistant (Phase 4). The browser calls the backend, which enforces per-plan monthly
 * credits, records usage, and proxies to the private AI worker. Requires a verified email
 * (FR-AUTH-1). Document-affecting suggestions are recorded to the disclosure ledger by the client
 * on accept (see DisclosureController).
 */
@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("isAuthenticated()")
@Tag(name = "AI Assistant", description = "Topic/objective/problem-statement generation, section "
        + "drafting, and the research alignment engine. Self-hosted model; per-plan credits.")
public class AiController {

    private final AiWorkerClient worker;
    private final ProfileService profiles;
    private final AiUsageService usage;
    private final String modelLabel;

    public AiController(AiWorkerClient worker, ProfileService profiles, AiUsageService usage,
                        @Value("${credresearch.ai.model-label:self-hosted}") String modelLabel) {
        this.worker = worker;
        this.profiles = profiles;
        this.usage = usage;
        this.modelLabel = modelLabel;
    }

    @GetMapping("/credits")
    @Operation(summary = "This month's AI credit usage for the caller's plan")
    public AiUsageService.CreditStatus credits() {
        TenantContext ctx = TenantContextHolder.require();
        return usage.status(ctx.userId(), ctx.plan());
    }

    private JsonNode call(String feature, JsonNode body) {
        TenantContext ctx = TenantContextHolder.require();
        if (!profiles.currentUser().isEmailVerified()) {
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED",
                    "Please verify your email before using AI features.");
        }
        usage.assertWithinCredits(ctx.userId(), ctx.plan());

        UUID projectId = uuid(body, "projectId");
        UUID documentId = uuid(body, "documentId");
        UUID requestId = usage.recordRequest(ctx.institutionId(), projectId, documentId,
                ctx.userId(), feature, modelLabel);
        try {
            JsonNode resp = worker.post(feature, body);
            usage.recordResponse(requestId, resp == null ? null : resp.toString(), "stop");
            return resp;
        } catch (RuntimeException e) {
            usage.markError(requestId);
            throw e;
        }
    }

    private static UUID uuid(JsonNode body, String field) {
        if (body == null || !body.hasNonNull(field)) {
            return null;
        }
        try {
            return UUID.fromString(body.get(field).asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @PostMapping("/topics")
    @Operation(summary = "Generate research topics + feasibility")
    public JsonNode topics(@RequestBody JsonNode body) {
        return call("topics", body);
    }

    @PostMapping("/objectives")
    @Operation(summary = "Generate aim, objectives, research questions & hypotheses")
    public JsonNode objectives(@RequestBody JsonNode body) {
        return call("objectives", body);
    }

    @PostMapping("/problem-statement")
    @Operation(summary = "Draft/refine a problem statement + significance")
    public JsonNode problemStatement(@RequestBody JsonNode body) {
        return call("problem-statement", body);
    }

    @PostMapping("/section-assist")
    @Operation(summary = "Draft or improve a document section")
    public JsonNode sectionAssist(@RequestBody JsonNode body) {
        return call("section-assist", body);
    }

    @PostMapping("/alignment")
    @Operation(summary = "Research alignment engine: score + findings")
    public JsonNode alignment(@RequestBody JsonNode body) {
        return call("alignment", body);
    }
}
