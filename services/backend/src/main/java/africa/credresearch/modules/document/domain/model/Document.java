package africa.credresearch.modules.document.domain.model;

import java.util.UUID;

/** A document instantiated from a template within a project (FR-DOC-1). */
public record Document(UUID id, UUID projectId, UUID templateId, String title, String status) {}
