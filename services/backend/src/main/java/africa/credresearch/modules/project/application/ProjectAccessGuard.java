package africa.credresearch.modules.project.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import africa.credresearch.modules.project.domain.port.ProjectRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Enforces tenant + membership on every project-scoped operation (FR-TEN-1, FR-PROJ-2).
 * Loads the project within the caller's tenant, then verifies the caller is a member with one of
 * the required project-roles. Platform admins bypass membership but still resolve a real project.
 */
@Component
public class ProjectAccessGuard {

    private final ProjectRepository projects;
    private final ProjectMemberRepository members;

    public ProjectAccessGuard(ProjectRepository projects, ProjectMemberRepository members) {
        this.projects = projects;
        this.members = members;
    }

    /** Loads the project in the caller's tenant or throws 404 (also blocks cross-tenant access). */
    public Project requireProject(UUID projectId) {
        TenantContext ctx = TenantContextHolder.require();
        return projects.findByIdAndInstitution(projectId, ctx.institutionId())
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "Project not found"));
    }

    /** Requires the caller to be any project member (or platform admin). Returns the project. */
    public Project requireMember(UUID projectId) {
        return requireRole(projectId);
    }

    /**
     * Requires the caller to be a project member holding one of {@code allowedRoles} (or platform
     * admin). Pass no roles to require mere membership. Throws 403 otherwise.
     */
    public Project requireRole(UUID projectId, ProjectMemberRole... allowedRoles) {
        TenantContext ctx = TenantContextHolder.require();
        Project project = requireProject(projectId);
        if (ctx.isPlatformAdmin()) {
            return project;
        }
        ProjectMember membership = members.findByProjectAndUser(projectId, ctx.userId())
                .orElseThrow(() -> ApiException.forbidden(
                        "NOT_PROJECT_MEMBER", "You are not a member of this project"));
        if (allowedRoles.length > 0 && !Set.of(allowedRoles).contains(membership.role())) {
            throw ApiException.forbidden("INSUFFICIENT_PROJECT_ROLE",
                    "Your project role does not permit this action");
        }
        return project;
    }
}
