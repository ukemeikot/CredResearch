package africa.credresearch.modules.project.domain.port;

import africa.credresearch.modules.project.domain.model.ProjectMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository {

    ProjectMember add(ProjectMember member);

    Optional<ProjectMember> findByProjectAndUser(UUID projectId, UUID userId);

    List<ProjectMember> findByProject(UUID projectId);

    boolean existsByProjectAndUser(UUID projectId, UUID userId);

    void remove(UUID projectId, UUID userId);

    long countByProject(UUID projectId);
}
