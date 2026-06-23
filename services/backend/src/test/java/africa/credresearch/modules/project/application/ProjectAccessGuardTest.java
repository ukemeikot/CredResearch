package africa.credresearch.modules.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import africa.credresearch.modules.project.domain.port.ProjectRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Membership enforcement tests (FR-TEN-1, FR-PROJ-2). */
class ProjectAccessGuardTest {

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ProjectMemberRepository members = mock(ProjectMemberRepository.class);
    private final ProjectAccessGuard guard = new ProjectAccessGuard(projects, members);

    private final UUID institution = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    private void asUser(UUID uid, UUID inst, String... roles) {
        TenantContextHolder.set(new TenantContext(uid, inst, Set.of(roles), "FREE"));
    }

    private void projectExists() {
        when(projects.findByIdAndInstitution(projectId, institution)).thenReturn(Optional.of(
                new Project(projectId, institution, null, userId, "T", "UG", ProjectStatus.DRAFT, null)));
    }

    @Test
    void nonMember_isForbidden() {
        asUser(userId, institution, "STUDENT");
        projectExists();
        when(members.findByProjectAndUser(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireMember(projectId))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NOT_PROJECT_MEMBER");
    }

    @Test
    void member_isAllowed() {
        asUser(userId, institution, "STUDENT");
        projectExists();
        when(members.findByProjectAndUser(projectId, userId)).thenReturn(Optional.of(
                new ProjectMember(UUID.randomUUID(), projectId, userId, ProjectMemberRole.OWNER)));

        Project p = guard.requireMember(projectId);
        assertThat(p.id()).isEqualTo(projectId);
    }

    @Test
    void member_withWrongRole_isForbidden() {
        asUser(userId, institution, "STUDENT");
        projectExists();
        when(members.findByProjectAndUser(projectId, userId)).thenReturn(Optional.of(
                new ProjectMember(UUID.randomUUID(), projectId, userId, ProjectMemberRole.VIEWER)));

        assertThatThrownBy(() -> guard.requireRole(projectId, ProjectMemberRole.OWNER))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_PROJECT_ROLE");
    }

    @Test
    void platformAdmin_bypassesMembership() {
        asUser(UUID.randomUUID(), institution, "PLATFORM_ADMIN");
        projectExists();

        Project p = guard.requireRole(projectId, ProjectMemberRole.OWNER);
        assertThat(p.id()).isEqualTo(projectId);
    }

    @Test
    void missingProjectInTenant_isNotFound() {
        asUser(userId, institution, "STUDENT");
        when(projects.findByIdAndInstitution(projectId, institution)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireMember(projectId))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("PROJECT_NOT_FOUND");
    }
}
