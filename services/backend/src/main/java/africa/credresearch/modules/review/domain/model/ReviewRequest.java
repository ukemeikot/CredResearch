package africa.credresearch.modules.review.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A request to review a section (or whole document) — Phase 6, FR-SUP-3. */
public record ReviewRequest(
        UUID id, UUID documentId, UUID documentSectionId, UUID requestedBy,
        UUID reviewerUserId, String reviewerEmail, String status, String note,
        Instant createdAt, Instant decidedAt) {}
