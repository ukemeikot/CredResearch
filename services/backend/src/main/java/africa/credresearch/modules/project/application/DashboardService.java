package africa.credresearch.modules.project.application;

import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Milestone;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.port.MilestoneRepository;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Aggregates project progress for the dashboard (FR-PROJ-7). */
@Service
public class DashboardService {

    private static final String COMPLETED = "COMPLETED";

    private final MilestoneRepository milestones;
    private final ProjectMemberRepository members;
    private final ProjectAccessGuard accessGuard;

    public DashboardService(MilestoneRepository milestones,
                            ProjectMemberRepository members,
                            ProjectAccessGuard accessGuard) {
        this.milestones = milestones;
        this.members = members;
        this.accessGuard = accessGuard;
    }

    /** Aggregate dashboard summary for a project (FR-PROJ-7). */
    public record Dashboard(
            ProjectStatus status,
            long totalMilestones,
            long completedMilestones,
            Milestone nextMilestone,
            long memberCount) {}

    public Dashboard forProject(UUID projectId) {
        Project project = accessGuard.requireMember(projectId);
        long total = milestones.countByProject(projectId);
        long completed = milestones.countByProjectAndStatus(projectId, COMPLETED);
        List<Milestone> upcoming = milestones.findUpcomingByProject(projectId);
        Milestone next = upcoming.isEmpty() ? null : upcoming.get(0);
        long memberCount = members.countByProject(projectId);
        return new Dashboard(project.status(), total, completed, next, memberCount);
    }
}
