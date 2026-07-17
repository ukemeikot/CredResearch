package africa.credresearch.modules.review.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An inline review comment, optionally anchored to a text range — FR-SUP-4/6. */
public record ReviewComment(
        UUID id, UUID reviewRequestId, UUID authorUserId, String authorLabel,
        Integer anchorStart, Integer anchorEnd, String quote, String body,
        boolean resolved, Instant createdAt) {}
