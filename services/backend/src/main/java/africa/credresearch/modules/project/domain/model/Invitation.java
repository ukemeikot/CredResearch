package africa.credresearch.modules.project.domain.model;

import africa.credresearch.modules.project.domain.InvitationStatus;
import java.time.Instant;
import java.util.UUID;

/** An email invitation to join a project (token/magic-link). */
public record Invitation(
        UUID id, UUID institutionId, UUID projectId, String email, String roleCode,
        InvitationStatus status, Instant expiresAt, UUID acceptedUserId, Instant createdAt) {}
