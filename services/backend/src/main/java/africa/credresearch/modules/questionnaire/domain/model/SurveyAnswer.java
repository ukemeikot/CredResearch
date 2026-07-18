package africa.credresearch.modules.questionnaire.domain.model;

import java.util.UUID;

/** One answer within a response; {@code valueJson} is the answer value as JSON (FR-Q). */
public record SurveyAnswer(UUID id, UUID surveyResponseId, UUID questionId, String valueJson) {}
