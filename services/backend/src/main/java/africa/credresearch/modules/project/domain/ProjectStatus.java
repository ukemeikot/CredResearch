package africa.credresearch.modules.project.domain;

import java.util.Map;
import java.util.Set;

/**
 * Project lifecycle status (FR-PROJ-4). Legal order:
 * DRAFT → PROPOSAL → IN_PROGRESS → UNDER_REVIEW → REVISIONS → APPROVED → COMPLETED.
 * REVISIONS and UNDER_REVIEW may bounce back and forth; all other backward/skip jumps are rejected.
 */
public enum ProjectStatus {
    DRAFT,
    PROPOSAL,
    IN_PROGRESS,
    UNDER_REVIEW,
    REVISIONS,
    APPROVED,
    COMPLETED;

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED = Map.of(
            DRAFT, Set.of(PROPOSAL),
            PROPOSAL, Set.of(IN_PROGRESS),
            IN_PROGRESS, Set.of(UNDER_REVIEW),
            UNDER_REVIEW, Set.of(REVISIONS, APPROVED),
            REVISIONS, Set.of(UNDER_REVIEW),
            APPROVED, Set.of(COMPLETED),
            COMPLETED, Set.of());

    /** True if {@code target} is a legal next status from this one. */
    public boolean canTransitionTo(ProjectStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
