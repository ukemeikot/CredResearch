package africa.credresearch.modules.document.domain.model;

import java.util.UUID;

/** Format rules attached to a template; applied on export (FR-DOC-5). */
public record FormatRule(
        UUID id, UUID templateId, String fontFamily, java.math.BigDecimal fontSizePt,
        java.math.BigDecimal lineSpacing, String marginsJson, String headingNumbering, String citationStyle) {}
