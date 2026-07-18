package africa.credresearch.modules.questionnaire.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A survey/questionnaire attached to a project (Phase 7, FR-Q). */
public record Questionnaire(UUID id, UUID projectId, String title, String consentText, String status, Instant createdAt) {}
