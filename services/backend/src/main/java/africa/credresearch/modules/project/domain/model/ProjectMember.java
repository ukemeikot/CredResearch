package africa.credresearch.modules.project.domain.model;

import africa.credresearch.modules.project.domain.ProjectMemberRole;
import java.util.UUID;

/** A user's membership in a project with a project-role (FR-PROJ-2/3). */
public record ProjectMember(
        UUID id,
        UUID projectId,
        UUID userId,
        ProjectMemberRole role) {}
