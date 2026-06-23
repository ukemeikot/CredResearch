package africa.credresearch.modules.project.domain.port;

import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    Project create(Project project, UUID createdBy);

    Optional<Project> findByIdAndInstitution(UUID id, UUID institutionId);

    /** Projects in a tenant the given user is a member of, paginated (tenant + membership scoped). */
    List<Project> findByInstitutionAndMember(UUID institutionId, UUID userId, int limit, int offset);

    /** All projects in a tenant, paginated (platform-admin / cross-member listing). */
    List<Project> findByInstitution(UUID institutionId, int limit, int offset);

    void update(UUID id, UUID institutionId, String title, String level, String abstractText,
                UUID departmentId, UUID updatedBy);

    void updateStatus(UUID id, UUID institutionId, ProjectStatus status, UUID updatedBy);

    long countByInstitution(UUID institutionId);
}
