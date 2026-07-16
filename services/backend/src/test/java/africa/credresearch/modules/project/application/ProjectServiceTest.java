package africa.credresearch.modules.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import africa.credresearch.modules.project.domain.port.ProjectRepository;
import africa.credresearch.modules.project.domain.port.StatusHistoryRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Happy-path tests for project create/list with mocked ports (FR-PROJ-1). */
class ProjectServiceTest {

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ProjectMemberRepository members = mock(ProjectMemberRepository.class);
    private final StatusHistoryRepository statusHistory = mock(StatusHistoryRepository.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final ProjectAccessGuard accessGuard = mock(ProjectAccessGuard.class);

    private final ProjectService service = new ProjectService(
            projects, members, statusHistory, activityService, accessGuard);

    private final UUID institution = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    private void asUser(String... roles) {
        TenantContextHolder.set(new TenantContext(userId, institution, Set.of(roles), "FREE"));
    }

    @Test
    void create_seedsOwnerMembershipAndHistory() {
        asUser("STUDENT");
        UUID projectId = UUID.randomUUID();
        when(projects.create(any(Project.class), eq(userId))).thenReturn(
                new Project(projectId, institution, null, userId, "My Project", "UG",
                        ProjectStatus.DRAFT, null));

        Project created = service.create("My Project", "UG", null, null);

        assertThat(created.id()).isEqualTo(projectId);
        assertThat(created.status()).isEqualTo(ProjectStatus.DRAFT);
        verify(members).add(any(ProjectMember.class));
        verify(statusHistory).record(eq(projectId), eq(null), eq(ProjectStatus.DRAFT), eq(userId));
        verify(activityService).record(eq(projectId), eq(userId), eq("PROJECT_CREATED"), any());
    }

    @Test
    void list_member_usesMembershipScopedFinder() {
        asUser("STUDENT");
        when(projects.findByInstitutionAndMember(eq(institution), eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of(new Project(UUID.randomUUID(), institution, null, userId,
                        "P", "UG", ProjectStatus.DRAFT, null)));

        List<Project> result = service.list(20, 0);

        assertThat(result).hasSize(1);
        verify(projects).findByInstitutionAndMember(institution, userId, 20, 0);
    }

    @Test
    void list_platformAdmin_seesWholeTenant() {
        asUser("PLATFORM_ADMIN");
        when(projects.findByInstitution(eq(institution), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.list(10, 0);

        verify(projects).findByInstitution(institution, 10, 0);
    }

    @Test
    void get_delegatesToAccessGuard() {
        asUser("STUDENT");
        UUID projectId = UUID.randomUUID();
        Project project = new Project(projectId, institution, null, userId, "P", "UG",
                ProjectStatus.DRAFT, null);
        when(accessGuard.requireMember(projectId)).thenReturn(project);

        assertThat(service.get(projectId)).isEqualTo(project);
    }

    @Test
    void transitionStatus_legal_writesHistoryAndActivity() {
        asUser("STUDENT");
        UUID projectId = UUID.randomUUID();
        Project draft = new Project(projectId, institution, null, userId, "P", "UG",
                ProjectStatus.DRAFT, null);
        when(accessGuard.requireRole(eq(projectId), any(ProjectMemberRole.class),
                any(ProjectMemberRole.class))).thenReturn(draft);
        when(projects.findByIdAndInstitution(projectId, institution)).thenReturn(
                java.util.Optional.of(new Project(projectId, institution, null, userId, "P", "UG",
                        ProjectStatus.PROPOSAL, null)));

        Project result = service.transitionStatus(projectId, ProjectStatus.PROPOSAL);

        assertThat(result.status()).isEqualTo(ProjectStatus.PROPOSAL);
        verify(projects).updateStatus(projectId, institution, ProjectStatus.PROPOSAL, userId);
        verify(statusHistory).record(projectId, ProjectStatus.DRAFT, ProjectStatus.PROPOSAL, userId);
    }
}
