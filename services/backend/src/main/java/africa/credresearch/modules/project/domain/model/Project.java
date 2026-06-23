package africa.credresearch.modules.project.domain.model;

import africa.credresearch.modules.project.domain.ProjectStatus;
import java.util.UUID;

/** A research project workspace (FR-PROJ-1). Framework-free domain model. */
public record Project(
        UUID id,
        UUID institutionId,
        UUID departmentId,
        UUID ownerUserId,
        String title,
        String level,
        ProjectStatus status,
        String abstractText) {}
