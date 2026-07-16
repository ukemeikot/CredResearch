package africa.credresearch.modules.document.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A historical snapshot of a section's content (FR-DOC-4). */
public record DocumentVersion(
        UUID id, UUID documentSectionId, int version, String content, String contentText,
        UUID authoredBy, Instant createdAt) {}
