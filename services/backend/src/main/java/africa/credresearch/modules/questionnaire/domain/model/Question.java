package africa.credresearch.modules.questionnaire.domain.model;

import java.util.UUID;

/** A question; {@code optionsJson} holds choices/likert scale as JSON (FR-Q). */
public record Question(UUID id, UUID questionnaireId, int orderIndex, String type, String prompt,
                       String optionsJson, boolean required) {}
