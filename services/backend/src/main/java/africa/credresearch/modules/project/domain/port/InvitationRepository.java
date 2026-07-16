package africa.credresearch.modules.project.domain.port;

import africa.credresearch.modules.project.domain.model.Invitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {

    Invitation create(Invitation invitation, String tokenHash, UUID createdBy);

    List<Invitation> findPendingByProject(UUID projectId);

    Optional<Invitation> findPendingByProjectAndEmail(UUID projectId, String email);

    void markExpired(UUID id);

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndProject(UUID id, UUID projectId);

    void markAccepted(UUID id, UUID acceptedUserId);

    void markRevoked(UUID id);
}
