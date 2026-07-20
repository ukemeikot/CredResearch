package africa.credresearch.modules.analysis.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.ai.application.AiUsageService;
import africa.credresearch.modules.ai.infrastructure.AiWorkerClient;
import africa.credresearch.modules.analysis.infrastructure.AnalysisClient;
import africa.credresearch.modules.identity.application.ProfileService;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Descriptive data analysis (Phase 8, FR-DATA): compute stats from a CSV (worker/pandas), then
 * optionally produce a grounded AI interpretation / Chapter 4 draft that cite only the computed
 * numbers. Membership-gated; AI steps are credit-metered + verified-email gated. Synchronous
 * (no persisted jobs in this increment).
 */
@Service
public class DataAnalysisService {

    private final AnalysisClient analysis;
    private final AiWorkerClient worker;
    private final AiUsageService usage;
    private final ProfileService profiles;
    private final ProjectAccessGuard projectAccess;
    private final ObjectMapper mapper;
    private final String modelLabel;

    public DataAnalysisService(AnalysisClient analysis, AiWorkerClient worker, AiUsageService usage,
                               ProfileService profiles, ProjectAccessGuard projectAccess, ObjectMapper mapper,
                               @Value("${credresearch.ai.model-label:self-hosted}") String modelLabel) {
        this.analysis = analysis;
        this.worker = worker;
        this.usage = usage;
        this.profiles = profiles;
        this.projectAccess = projectAccess;
        this.mapper = mapper;
        this.modelLabel = modelLabel;
    }

    public JsonNode describe(UUID projectId, byte[] csv) {
        projectAccess.requireMember(projectId);
        if (csv == null || csv.length == 0) {
            throw ApiException.badRequest("EMPTY_FILE", "The uploaded file is empty.");
        }
        return analysis.describe(csv);
    }

    public JsonNode interpret(UUID projectId, String topic, JsonNode stats) {
        return aiStep(projectId, topic, stats, "interpret-data");
    }

    public JsonNode chapter4(UUID projectId, String topic, JsonNode stats) {
        return aiStep(projectId, topic, stats, "chapter4");
    }

    private JsonNode aiStep(UUID projectId, String topic, JsonNode stats, String feature) {
        projectAccess.requireMember(projectId);
        if (!profiles.currentUser().isEmailVerified()) {
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED", "Please verify your email before using AI features.");
        }
        TenantContext ctx = TenantContextHolder.require();
        usage.assertWithinCredits(ctx.userId(), ctx.plan());
        ObjectNode body = mapper.createObjectNode();
        body.put("topic", topic == null ? "" : topic);
        body.set("stats", stats == null ? mapper.createObjectNode() : stats);
        UUID requestId = usage.recordRequest(ctx.institutionId(), projectId, null, ctx.userId(), feature, modelLabel);
        try {
            JsonNode resp = worker.post(feature, body);
            usage.recordResponse(requestId, resp == null ? null : resp.toString(), "stop");
            return resp;
        } catch (RuntimeException e) {
            usage.markError(requestId);
            throw e;
        }
    }
}
