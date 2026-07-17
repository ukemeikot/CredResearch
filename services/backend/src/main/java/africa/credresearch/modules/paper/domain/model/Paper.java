package africa.credresearch.modules.paper.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An uploaded source paper with extracted bibliographic metadata (Phase 5, FR-LIT-1/2). */
public record Paper(
        UUID id,
        UUID projectId,
        UUID uploadedBy,
        String filename,
        String title,
        String authors,
        Integer year,
        String doi,
        String journal,
        String extractionStatus,
        Instant createdAt,
        String summaryJson) {}
