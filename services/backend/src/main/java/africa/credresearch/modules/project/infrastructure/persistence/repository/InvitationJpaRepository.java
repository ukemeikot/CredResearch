package africa.credresearch.modules.project.infrastructure.persistence.repository;

import africa.credresearch.modules.project.infrastructure.persistence.entity.InvitationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationJpaRepository extends JpaRepository<InvitationEntity, UUID> {
    List<InvitationEntity> findByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, String status);
    Optional<InvitationEntity> findFirstByProjectIdAndEmailAndStatus(UUID projectId, String email, String status);
    Optional<InvitationEntity> findByTokenHash(String tokenHash);
    Optional<InvitationEntity> findByIdAndProjectId(UUID id, UUID projectId);
}
