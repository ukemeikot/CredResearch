package africa.credresearch.modules.project.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An entry in a project's activity feed (FR-PROJ-6). {@code payload} is raw JSON. */
public record Activity(
        UUID id,
        UUID projectId,
        UUID actorUserId,
        String type,
        String payload,
        Instant createdAt) {}
