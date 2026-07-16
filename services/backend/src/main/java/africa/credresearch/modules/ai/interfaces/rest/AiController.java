package africa.credresearch.modules.ai.interfaces.rest;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.ai.infrastructure.AiWorkerClient;
import africa.credresearch.modules.identity.application.ProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI Research Assistant (Phase 4). The browser calls the backend, which proxies to the private
 * AI worker. Requires a verified email (FR-AUTH-1: verify before AI). Usage tracking, per-plan
 * credits and disclosure-ledger writes are layered on top of this proxy.
 */
@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("isAuthenticated()")
@Tag(name = "AI Assistant", description = "Topic/objective/problem-statement generation, section "
        + "drafting assistance, and the research alignment engine. Backed by a self-hosted model.")
public class AiController {

    private final AiWorkerClient worker;
    private final ProfileService profiles;

    public AiController(AiWorkerClient worker, ProfileService profiles) {
        this.worker = worker;
        this.profiles = profiles;
    }

    private void requireVerified() {
        if (!profiles.currentUser().isEmailVerified()) {
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED",
                    "Please verify your email before using AI features.");
        }
    }

    private JsonNode call(String path, JsonNode body) {
        requireVerified();
        return worker.post(path, body);
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
