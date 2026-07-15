package africa.credresearch.modules.project.interfaces.rest;

import africa.credresearch.modules.project.application.ActivityService;
import africa.credresearch.modules.project.application.DashboardService;
import africa.credresearch.modules.project.application.MemberService;
import africa.credresearch.modules.project.application.MilestoneService;
import africa.credresearch.modules.project.application.ProjectService;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Activity;
import africa.credresearch.modules.project.domain.model.Milestone;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Projects", description = "Project workspace. Tenant-scoped; every operation also "
        + "enforces project membership and project-role (OWNER/SUPERVISOR/CONSULTANT/VIEWER).")
public class ProjectController {

    private final ProjectService projectService;
    private final MemberService memberService;
    private final MilestoneService milestoneService;
    private final ActivityService activityService;
    private final DashboardService dashboardService;

    public ProjectController(ProjectService projectService,
                            MemberService memberService,
                            MilestoneService milestoneService,
                            ActivityService activityService,
                            DashboardService dashboardService) {
        this.projectService = projectService;
        this.memberService = memberService;
        this.milestoneService = milestoneService;
        this.activityService = activityService;
        this.dashboardService = dashboardService;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    public record ProjectResponse(UUID id, UUID institutionId, UUID departmentId, UUID ownerUserId,
                                  String title, String level, ProjectStatus status, String abstractText) {
        static ProjectResponse from(Project p) {
            return new ProjectResponse(p.id(), p.institutionId(), p.departmentId(), p.ownerUserId(),
                    p.title(), p.level(), p.status(), p.abstractText());
        }
    }

    public record MilestoneResponse(UUID id, UUID projectId, String title, LocalDate dueDate,
                                    String status, Instant completedAt) {
        static MilestoneResponse from(Milestone m) {
            return new MilestoneResponse(m.id(), m.projectId(), m.title(), m.dueDate(),
                    m.status(), m.completedAt());
        }
    }

    public record MemberResponse(UUID id, UUID projectId, UUID userId, ProjectMemberRole role) {
        static MemberResponse from(ProjectMember m) {
            return new MemberResponse(m.id(), m.projectId(), m.userId(), m.role());
        }
    }

    public record ActivityResponse(UUID id, UUID projectId, UUID actorUserId, String type,
                                   String payload, Instant createdAt) {
        static ActivityResponse from(Activity a) {
            return new ActivityResponse(a.id(), a.projectId(), a.actorUserId(), a.type(),
                    a.payload(), a.createdAt());
        }
    }

    public record DashboardResponse(ProjectStatus status, long totalMilestones, long completedMilestones,
                                    MilestoneResponse nextMilestone, long memberCount) {
        static DashboardResponse from(DashboardService.Dashboard d) {
            return new DashboardResponse(d.status(), d.totalMilestones(), d.completedMilestones(),
                    d.nextMilestone() == null ? null : MilestoneResponse.from(d.nextMilestone()),
                    d.memberCount());
        }
    }

    public record ProjectDetailResponse(ProjectResponse project, DashboardResponse dashboard,
                                        List<MemberResponse> members, List<MilestoneResponse> milestones) {}

    public record CreateProjectRequest(@NotBlank String title, String level, UUID departmentId,
                                       String abstractText) {}

    public record UpdateProjectRequest(String title, String level, String abstractText, UUID departmentId) {}

    public record AddMemberRequest(@NotNull UUID userId, @NotNull ProjectMemberRole role) {}

    public record AddMilestoneRequest(@NotBlank String title, LocalDate dueDate, String status) {}

    public record TransitionStatusRequest(@NotNull ProjectStatus status) {}

    // ── Endpoints ─────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Create a project",
            description = "Creates a project in the caller's tenant and adds the caller as OWNER (FR-PROJ-1).")
    @ApiResponse(responseCode = "200", description = "Created project")
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest req) {
        return ProjectResponse.from(projectService.create(
                req.title(), req.level(), req.departmentId(), req.abstractText()));
    }

    @GetMapping
    @Operation(summary = "List visible projects",
            description = "Projects the caller is a member of (platform admins see the whole tenant). Paginated.")
    @ApiResponse(responseCode = "200", description = "Projects")
    public List<ProjectResponse> list(@RequestParam(defaultValue = "20") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return projectService.list(limit, offset).stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Project detail + dashboard",
            description = "Returns the project with its dashboard, members and milestones. Member-only (FR-PROJ-7).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project detail"),
            @ApiResponse(responseCode = "403", description = "Not a project member", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Not found in this tenant", content = @Content())
    })
    public ProjectDetailResponse get(@PathVariable UUID id) {
        Project project = projectService.get(id);
        DashboardResponse dashboard = DashboardResponse.from(dashboardService.forProject(id));
        List<MemberResponse> members = memberService.list(id).stream().map(MemberResponse::from).toList();
        List<MilestoneResponse> milestones = milestoneService.list(id).stream()
                .map(MilestoneResponse::from).toList();
        return new ProjectDetailResponse(ProjectResponse.from(project), dashboard, members, milestones);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a project", description = "Requires the OWNER project-role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated project"),
            @ApiResponse(responseCode = "403", description = "Insufficient project role", content = @Content())
    })
    public ProjectResponse update(@PathVariable UUID id, @RequestBody UpdateProjectRequest req) {
        return ProjectResponse.from(projectService.update(
                id, req.title(), req.level(), req.abstractText(), req.departmentId()));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member or (co-)supervisor",
            description = "Requires OWNER. Multiple SUPERVISORs (co-supervisors) are allowed (FR-PROJ-3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Added member"),
            @ApiResponse(responseCode = "409", description = "User already a member", content = @Content())
    })
    public MemberResponse addMember(@PathVariable UUID id, @Valid @RequestBody AddMemberRequest req) {
        return MemberResponse.from(memberService.addMember(id, req.userId(), req.role()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member", description = "Requires OWNER. The owner cannot be removed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Member not found", content = @Content())
    })
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        memberService.removeMember(id, userId);
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "Add a milestone", description = "Requires OWNER or SUPERVISOR (FR-PROJ-5).")
    @ApiResponse(responseCode = "200", description = "Added milestone")
    public MilestoneResponse addMilestone(@PathVariable UUID id, @Valid @RequestBody AddMilestoneRequest req) {
        return MilestoneResponse.from(milestoneService.add(id, req.title(), req.dueDate(), req.status()));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Activity feed", description = "Member-only. Paginated, newest first (FR-PROJ-6).")
    @ApiResponse(responseCode = "200", description = "Activity entries")
    public List<ActivityResponse> activities(@PathVariable UUID id,
                                             @RequestParam(defaultValue = "50") int limit,
                                             @RequestParam(defaultValue = "0") int offset) {
        projectService.get(id); // enforce tenant + membership before reading the feed
        return activityService.list(id, limit, offset).stream().map(ActivityResponse::from).toList();
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Transition project status",
            description = "Enforces the legal lifecycle and records status history (FR-PROJ-4). "
                    + "Requires OWNER or SUPERVISOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New status"),
            @ApiResponse(responseCode = "400", description = "Illegal transition", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Insufficient project role", content = @Content())
    })
    public ProjectResponse transitionStatus(@PathVariable UUID id,
                                            @Valid @RequestBody TransitionStatusRequest req) {
        return ProjectResponse.from(projectService.transitionStatus(id, req.status()));
    }
}
