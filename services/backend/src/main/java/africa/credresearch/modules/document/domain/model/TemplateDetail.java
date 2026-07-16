package africa.credresearch.modules.document.domain.model;

import java.util.List;

/** A template with its sections and format rule. */
public record TemplateDetail(Template template, List<TemplateSection> sections, FormatRule formatRule) {}
