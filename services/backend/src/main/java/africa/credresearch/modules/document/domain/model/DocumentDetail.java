package africa.credresearch.modules.document.domain.model;

import java.util.List;

/** A document with its sections (single fetch, no waterfall). */
public record DocumentDetail(Document document, List<DocumentSection> sections) {}
