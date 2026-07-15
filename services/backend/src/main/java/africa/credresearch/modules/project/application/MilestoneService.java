package africa.credresearch.modules.project.application;

import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.model.Milestone;
import africa.credresearch.modules.project.domain.port.MilestoneRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Project milestones (FR-PROJ-5). */
@Service
public class MilestoneService {

    private final MilestoneRepository milestones;
    private final ProjectAccessGuard accessGuard;
    private final ActivityService activityService;

    public MilestoneService(MilestoneRepository milestones,
                            ProjectAccessGuard accessGuard,
                            ActivityService activityService) {
        this.milestones = milestones;
        this.accessGuard = accessGuard;
        this.activityService = activityService;
    }

    /** Adds a milestone. Requires OWNER or SUPERVISOR project-role. */
    @Transactional
    public Milestone add(UUID projectId, String title, LocalDate dueDate, String status) {
        TenantContext ctx = TenantContextHolder.require();
        accessGuard.requireRole(projectId, ProjectMemberRole.OWNER, ProjectMemberRole.SUPERVISOR);
        Milestone added = milestones.add(new Milestone(null, projectId, title, dueDate,
                status == null ? "PENDING" : status, null));
        activityService.record(projectId, ctx.userId(), "MILESTONE_ADDED",
                Map.of("title", title));
        return added;
    }

    public List<Milestone> list(UUID projectId) {
        accessGuard.requireMember(projectId);
        return milestones.findByProject(projectId);
    }
}
