package africa.credresearch.modules.project.application;

import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import africa.credresearch.modules.project.domain.port.ProjectRepository;
import africa.credresearch.modules.project.domain.port.StatusHistoryRepository;
import africa.credresearch.common.error.ApiException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project lifecycle operations (FR-PROJ-1/4). Every read/write is tenant-scoped via
 * {@link TenantContextHolder}; membership is enforced by {@link ProjectAccessGuard}.
 */
@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final StatusHistoryRepository statusHistory;
    private final ActivityService activityService;
    private final ProjectAccessGuard accessGuard;
    private final ProjectDeletionService deletionService;

    public ProjectService(ProjectRepository projects,
                          ProjectMemberRepository members,
                          StatusHistoryRepository statusHistory,
                          ActivityService activityService,
                          ProjectAccessGuard accessGuard,
                          ProjectDeletionService deletionService) {
        this.projects = projects;
        this.members = members;
        this.statusHistory = statusHistory;
        this.activityService = activityService;
        this.accessGuard = accessGuard;
        this.deletionService = deletionService;
    }

    /** Creates a project owned by the caller and seeds an OWNER membership (FR-PROJ-1/2). */
    @Transactional
    public Project create(String title, String level, UUID departmentId, String abstractText) {
        TenantContext ctx = TenantContextHolder.require();
        Project created = projects.create(
                new Project(null, ctx.institutionId(), departmentId, ctx.userId(),
                        title, level, ProjectStatus.DRAFT, abstractText),
                ctx.userId());
        members.add(new ProjectMember(null, created.id(), ctx.userId(), ProjectMemberRole.OWNER));
        statusHistory.record(created.id(), null, ProjectStatus.DRAFT, ctx.userId());
        activityService.record(created.id(), ctx.userId(), "PROJECT_CREATED",
                Map.of("title", title));
        return created;
    }

    /** Returns the project if the caller is a member (or platform admin) within their tenant. */
    public Project get(UUID id) {
        return accessGuard.requireMember(id);
    }

    /** Lists projects the caller can see (their memberships; platform admins see the whole tenant). */
    public List<Project> list(int limit, int offset) {
        TenantContext ctx = TenantContextHolder.require();
        if (ctx.isPlatformAdmin()) {
            return projects.findByInstitution(ctx.institutionId(), limit, offset);
        }
        return projects.findByInstitutionAndMember(ctx.institutionId(), ctx.userId(), limit, offset);
    }

    /** Updates editable fields; requires OWNER project-role (or platform admin). */
    @Transactional
    public Project update(UUID id, String title, String level, String abstractText, UUID departmentId) {
        TenantContext ctx = TenantContextHolder.require();
        accessGuard.requireRole(id, ProjectMemberRole.OWNER);
        projects.update(id, ctx.institutionId(), title, level, abstractText, departmentId, ctx.userId());
        activityService.record(id, ctx.userId(), "PROJECT_UPDATED", Map.of());
        return projects.findByIdAndInstitution(id, ctx.institutionId()).orElseThrow();
    }

    /**
     * Permanently deletes the project and all of its data. Requires the OWNER project-role (or
     * platform admin); the cascade runs in one transaction (see {@link ProjectDeletionService}).
     */
    @Transactional
    public void delete(UUID id) {
        accessGuard.requireRole(id, ProjectMemberRole.OWNER);
        deletionService.deleteCascade(id);
    }

    /**
     * Transitions a project to {@code target}, enforcing the state machine (FR-PROJ-4) and writing
     * status history + an activity entry. Requires OWNER or SUPERVISOR project-role.
     */
    @Transactional
    public Project transitionStatus(UUID id, ProjectStatus target) {
        TenantContext ctx = TenantContextHolder.require();
        Project project = accessGuard.requireRole(id, ProjectMemberRole.OWNER, ProjectMemberRole.SUPERVISOR);
        ProjectStatus current = project.status();
        if (!current.canTransitionTo(target)) {
            throw ApiException.badRequest("ILLEGAL_STATUS_TRANSITION",
                    "Cannot transition from " + current + " to " + target);
        }
        projects.updateStatus(id, ctx.institutionId(), target, ctx.userId());
        statusHistory.record(id, current, target, ctx.userId());
        activityService.record(id, ctx.userId(), "STATUS_CHANGED",
                Map.of("from", current.name(), "to", target.name()));
        return projects.findByIdAndInstitution(id, ctx.institutionId()).orElseThrow();
    }
}
