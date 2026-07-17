package africa.credresearch.modules.ai.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.ai.infrastructure.persistence.entity.AiRequestEntity;
import africa.credresearch.modules.ai.infrastructure.persistence.entity.AiResponseEntity;
import africa.credresearch.modules.ai.infrastructure.persistence.repository.AiRequestJpaRepository;
import africa.credresearch.modules.ai.infrastructure.persistence.repository.AiResponseJpaRepository;
import africa.credresearch.modules.ai.infrastructure.persistence.repository.PlanJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI usage tracking + per-plan monthly credits (FR-AI). One successful AI call = one credit. */
@Service
public class AiUsageService {

    private static final int DEFAULT_CREDITS = 50;

    private final AiRequestJpaRepository requests;
    private final AiResponseJpaRepository responses;
    private final PlanJpaRepository plans;

    public AiUsageService(AiRequestJpaRepository requests, AiResponseJpaRepository responses,
                          PlanJpaRepository plans) {
        this.requests = requests;
        this.responses = responses;
        this.plans = plans;
    }

    public record CreditStatus(String plan, long used, int limit, long remaining) {}

    private static Instant startOfMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private int limitFor(String planCode) {
        return plans.findByCode(planCode == null ? "FREE" : planCode)
                .map(p -> p.getAiMonthlyCredits())
                .orElse(DEFAULT_CREDITS);
    }

    public CreditStatus status(UUID userId, String planCode) {
        long used = requests.countByUserIdAndStatusAndCreatedAtAfter(userId, "OK", startOfMonth());
        int limit = limitFor(planCode);
        return new CreditStatus(planCode == null ? "FREE" : planCode, used, limit, Math.max(0, limit - used));
    }

    /** Rejects the call if the caller has no monthly AI credits left. */
    public void assertWithinCredits(UUID userId, String planCode) {
        CreditStatus s = status(userId, planCode);
        if (s.remaining() <= 0) {
            throw ApiException.of(org.springframework.http.HttpStatus.PAYMENT_REQUIRED,
                    "AI_CREDITS_EXHAUSTED",
                    "You've used all %d AI credits for this month on the %s plan.".formatted(s.limit(), s.plan()));
        }
    }

    @Transactional
    public UUID recordRequest(UUID institutionId, UUID projectId, UUID documentId, UUID userId,
                              String featureKey, String model) {
        AiRequestEntity e = new AiRequestEntity();
        e.setInstitutionId(institutionId);
        e.setProjectId(projectId);
        e.setDocumentId(documentId);
        e.setUserId(userId);
        e.setFeatureKey(featureKey);
        e.setModel(model);
        e.setStatus("OK");
        return requests.save(e).getId();
    }

    @Transactional
    public void recordResponse(UUID requestId, String outputJson, String finishReason) {
        AiResponseEntity r = new AiResponseEntity();
        r.setAiRequestId(requestId);
        r.setOutputJson(outputJson);
        r.setFinishReason(finishReason);
        responses.save(r);
    }

    @Transactional
    public void markError(UUID requestId) {
        requests.findById(requestId).ifPresent(e -> {
            e.setStatus("ERROR");
            requests.save(e);
        });
    }
}
