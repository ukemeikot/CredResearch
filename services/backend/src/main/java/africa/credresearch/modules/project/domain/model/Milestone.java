package africa.credresearch.modules.project.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A project milestone with a due date and status (FR-PROJ-5). */
public record Milestone(
        UUID id,
        UUID projectId,
        String title,
        LocalDate dueDate,
        String status,
        Instant completedAt) {}
