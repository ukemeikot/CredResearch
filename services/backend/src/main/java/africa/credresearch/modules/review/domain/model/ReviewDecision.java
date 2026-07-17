package africa.credresearch.modules.review.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A recorded review decision (history preserved across rounds) — FR-SUP-5/6. */
public record ReviewDecision(
        UUID id, UUID reviewRequestId, String decision, String summary, UUID decidedBy, Instant createdAt) {}
