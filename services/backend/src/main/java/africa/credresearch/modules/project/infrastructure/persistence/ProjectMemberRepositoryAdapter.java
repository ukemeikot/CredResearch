package africa.credresearch.modules.project.infrastructure.persistence;

import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import africa.credresearch.modules.project.infrastructure.persistence.entity.ProjectMemberEntity;
import africa.credresearch.modules.project.infrastructure.persistence.repository.ProjectMemberJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProjectMemberRepositoryAdapter implements ProjectMemberRepository {

    private final ProjectMemberJpaRepository jpa;

    public ProjectMemberRepositoryAdapter(ProjectMemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ProjectMember add(ProjectMember member) {
        ProjectMemberEntity e = new ProjectMemberEntity();
        e.setProjectId(member.projectId());
        e.setUserId(member.userId());
        e.setRole(member.role().name());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<ProjectMember> findByProjectAndUser(UUID projectId, UUID userId) {
        return jpa.findByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId)
                .map(ProjectMemberRepositoryAdapter::toDomain);
    }

    @Override
    public List<ProjectMember> findByProject(UUID projectId) {
        return jpa.findByProjectIdAndDeletedAtIsNull(projectId).stream()
                .map(ProjectMemberRepositoryAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByProjectAndUser(UUID projectId, UUID userId) {
        return jpa.existsByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId);
    }

    @Override
    @Transactional
    public void remove(UUID projectId, UUID userId) {
        jpa.findByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId).ifPresent(e -> {
            e.setDeletedAt(e.getDeletedAt() == null ? Instant.now() : e.getDeletedAt());
            jpa.save(e);
        });
    }

    @Override
    public long countByProject(UUID projectId) {
        return jpa.countByProjectIdAndDeletedAtIsNull(projectId);
    }

    static ProjectMember toDomain(ProjectMemberEntity e) {
        return new ProjectMember(e.getId(), e.getProjectId(), e.getUserId(),
                ProjectMemberRole.valueOf(e.getRole()));
    }
}
