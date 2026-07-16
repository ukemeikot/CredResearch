package africa.credresearch.modules.project.infrastructure.persistence;

import africa.credresearch.modules.project.domain.ProjectStatus;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.port.ProjectRepository;
import africa.credresearch.modules.project.infrastructure.persistence.entity.ProjectEntity;
import africa.credresearch.modules.project.infrastructure.persistence.repository.ProjectJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpa;

    public ProjectRepositoryAdapter(ProjectJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Project create(Project project, UUID createdBy) {
        ProjectEntity e = new ProjectEntity();
        e.setInstitutionId(project.institutionId());
        e.setDepartmentId(project.departmentId());
        e.setOwnerUserId(project.ownerUserId());
        e.setTitle(project.title());
        e.setLevel(project.level());
        e.setStatus(project.status() == null ? ProjectStatus.DRAFT.name() : project.status().name());
        e.setAbstractText(project.abstractText());
        e.setCreatedBy(createdBy);
        e.setUpdatedBy(createdBy);
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Project> findByIdAndInstitution(UUID id, UUID institutionId) {
        return jpa.findByIdAndInstitutionIdAndDeletedAtIsNull(id, institutionId)
                .map(ProjectRepositoryAdapter::toDomain);
    }

    @Override
    public List<Project> findByInstitutionAndMember(UUID institutionId, UUID userId, int limit, int offset) {
        return jpa.findByInstitutionAndMember(institutionId, userId, pageable(limit, offset))
                .stream().map(ProjectRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Project> findByInstitution(UUID institutionId, int limit, int offset) {
        return jpa.findByInstitutionIdAndDeletedAtIsNull(institutionId, pageable(limit, offset))
                .stream().map(ProjectRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public void update(UUID id, UUID institutionId, String title, String level, String abstractText,
                       UUID departmentId, UUID updatedBy) {
        ProjectEntity e = jpa.findByIdAndInstitutionIdAndDeletedAtIsNull(id, institutionId).orElseThrow();
        if (title != null) e.setTitle(title);
        if (level != null) e.setLevel(level);
        if (abstractText != null) e.setAbstractText(abstractText);
        if (departmentId != null) e.setDepartmentId(departmentId);
        e.setUpdatedBy(updatedBy);
        jpa.save(e);
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, UUID institutionId, ProjectStatus status, UUID updatedBy) {
        ProjectEntity e = jpa.findByIdAndInstitutionIdAndDeletedAtIsNull(id, institutionId).orElseThrow();
        e.setStatus(status.name());
        e.setUpdatedBy(updatedBy);
        jpa.save(e);
    }

    @Override
    public long countByInstitution(UUID institutionId) {
        return jpa.countByInstitutionIdAndDeletedAtIsNull(institutionId);
    }

    private static PageRequest pageable(int limit, int offset) {
        int size = Math.max(1, limit);
        int page = limit <= 0 ? 0 : offset / size;
        return PageRequest.of(Math.max(0, page), size);
    }

    static Project toDomain(ProjectEntity e) {
        return new Project(
                e.getId(), e.getInstitutionId(), e.getDepartmentId(), e.getOwnerUserId(),
                e.getTitle(), e.getLevel(), ProjectStatus.valueOf(e.getStatus()), e.getAbstractText());
    }
}
