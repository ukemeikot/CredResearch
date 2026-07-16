package africa.credresearch.modules.project.infrastructure.persistence;

import africa.credresearch.modules.project.domain.InvitationStatus;
import africa.credresearch.modules.project.domain.model.Invitation;
import africa.credresearch.modules.project.domain.port.InvitationRepository;
import africa.credresearch.modules.project.infrastructure.persistence.entity.InvitationEntity;
import africa.credresearch.modules.project.infrastructure.persistence.repository.InvitationJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final InvitationJpaRepository jpa;

    public InvitationRepositoryAdapter(InvitationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Invitation create(Invitation inv, String tokenHash, UUID createdBy) {
        InvitationEntity e = new InvitationEntity();
        e.setInstitutionId(inv.institutionId());
        e.setProjectId(inv.projectId());
        e.setEmail(inv.email());
        e.setRoleCode(inv.roleCode());
        e.setTokenHash(tokenHash);
        e.setStatus(InvitationStatus.PENDING.name());
        e.setExpiresAt(inv.expiresAt());
        e.setCreatedBy(createdBy);
        return toDomain(jpa.save(e));
    }

    @Override
    public List<Invitation> findPendingByProject(UUID projectId) {
        return jpa.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, InvitationStatus.PENDING.name())
                .stream().map(InvitationRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Invitation> findPendingByProjectAndEmail(UUID projectId, String email) {
        return jpa.findFirstByProjectIdAndEmailAndStatus(projectId, email, InvitationStatus.PENDING.name())
                .map(InvitationRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void markExpired(UUID id) {
        InvitationEntity e = jpa.findById(id).orElseThrow();
        e.setStatus(InvitationStatus.EXPIRED.name());
        jpa.save(e);
    }

    @Override
    public Optional<Invitation> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(InvitationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Invitation> findByIdAndProject(UUID id, UUID projectId) {
        return jpa.findByIdAndProjectId(id, projectId).map(InvitationRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void markAccepted(UUID id, UUID acceptedUserId) {
        InvitationEntity e = jpa.findById(id).orElseThrow();
        e.setStatus(InvitationStatus.ACCEPTED.name());
        e.setAcceptedUserId(acceptedUserId);
        jpa.save(e);
    }

    @Override
    @Transactional
    public void markRevoked(UUID id) {
        InvitationEntity e = jpa.findById(id).orElseThrow();
        e.setStatus(InvitationStatus.REVOKED.name());
        jpa.save(e);
    }

    static Invitation toDomain(InvitationEntity e) {
        return new Invitation(e.getId(), e.getInstitutionId(), e.getProjectId(), e.getEmail(),
                e.getRoleCode(), InvitationStatus.valueOf(e.getStatus()), e.getExpiresAt(),
                e.getAcceptedUserId(), e.getCreatedAt());
    }
}
