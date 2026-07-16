package africa.credresearch.modules.document.domain.model;

import java.util.UUID;

/** An ordered section defined by a template (FR-TMPL-3). */
public record TemplateSection(
        UUID id, UUID templateId, int orderIndex, String chapter, String heading, String guidance) {}
