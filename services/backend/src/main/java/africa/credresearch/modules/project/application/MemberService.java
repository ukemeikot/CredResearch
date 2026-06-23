package africa.credresearch.modules.project.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages project membership, including co-supervisors (FR-PROJ-2/3). */
@Service
public class MemberService {

    private final ProjectMemberRepository members;
    private final ProjectAccessGuard accessGuard;
    private final ActivityService activityService;

    public MemberService(ProjectMemberRepository members,
                         ProjectAccessGuard accessGuard,
                         ActivityService activityService) {
        this.members = members;
        this.accessGuard = accessGuard;
        this.activityService = activityService;
    }

    /** Adds a member with the given project-role. Multiple SUPERVISORs (co-supervisors) allowed. */
    @Transactional
    public ProjectMember addMember(UUID projectId, UUID userId, ProjectMemberRole role) {
        TenantContext ctx = TenantContextHolder.require();
        accessGuard.requireRole(projectId, ProjectMemberRole.OWNER);
        if (members.existsByProjectAndUser(projectId, userId)) {
            throw ApiException.conflict("MEMBER_EXISTS", "User is already a member of this project");
        }
        ProjectMember added = members.add(new ProjectMember(null, projectId, userId, role));
        activityService.record(projectId, ctx.userId(), "MEMBER_ADDED",
                Map.of("userId", userId.toString(), "role", role.name()));
        return added;
    }

    public List<ProjectMember> list(UUID projectId) {
        accessGuard.requireMember(projectId);
        return members.findByProject(projectId);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        TenantContext ctx = TenantContextHolder.require();
        accessGuard.requireRole(projectId, ProjectMemberRole.OWNER);
        ProjectMember member = members.findByProjectAndUser(projectId, userId)
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "Member not found"));
        if (member.role() == ProjectMemberRole.OWNER) {
            throw ApiException.badRequest("CANNOT_REMOVE_OWNER", "The project owner cannot be removed");
        }
        members.remove(projectId, userId);
        activityService.record(projectId, ctx.userId(), "MEMBER_REMOVED",
                Map.of("userId", userId.toString()));
    }
}
