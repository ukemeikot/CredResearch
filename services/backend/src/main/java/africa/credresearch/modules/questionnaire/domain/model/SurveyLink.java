package africa.credresearch.modules.questionnaire.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A tokenized public survey link (FR-Q). The raw token is only returned once, at creation. */
public record SurveyLink(UUID id, UUID questionnaireId, boolean active, Instant expiresAt, Instant createdAt) {}
