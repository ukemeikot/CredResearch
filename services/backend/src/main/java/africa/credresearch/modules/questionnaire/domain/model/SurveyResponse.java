package africa.credresearch.modules.questionnaire.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A submitted survey response (FR-Q). */
public record SurveyResponse(UUID id, UUID surveyLinkId, boolean consentGiven, Instant submittedAt) {}
