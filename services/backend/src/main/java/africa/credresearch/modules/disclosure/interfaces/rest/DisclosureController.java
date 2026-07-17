package africa.credresearch.modules.disclosure.interfaces.rest;

import africa.credresearch.modules.disclosure.application.DisclosureService;
import africa.credresearch.modules.disclosure.infrastructure.persistence.entity.DisclosureEntryEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-Use Disclosure Ledger (FR-LEDGER). Members read a document's ledger; the editor appends an
 * entry when a user accepts/edits/rejects AI-suggested content.
 */
@RestController
@RequestMapping("/api/v1/disclosure")
@PreAuthorize("isAuthenticated()")
@Tag(name = "AI Disclosure", description = "Append-only, hash-chained record of AI assistance per document.")
public class DisclosureController {

    private final DisclosureService service;

    public DisclosureController(DisclosureService service) {
        this.service = service;
    }

    public record EntryResponse(UUID id, UUID documentSectionId, String featureKey, String model,
                                String suggestionSummary, String action, String entryHash, Instant createdAt) {
        static EntryResponse from(DisclosureEntryEntity e) {
            return new EntryResponse(e.getId(), e.getDocumentSectionId(), e.getFeatureKey(), e.getModel(),
                    e.getSuggestionSummary(), e.getAction(), e.getEntryHash(), e.getCreatedAt());
        }
    }

    public record AppendRequest(UUID documentSectionId, UUID aiRequestId, @NotBlank String featureKey,
                                String model, String suggestionSummary, String action) {}

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "List a document's AI-use disclosure ledger")
    public List<EntryResponse> list(@PathVariable UUID documentId) {
        return service.list(documentId).stream().map(EntryResponse::from).toList();
    }

    @PostMapping("/documents/{documentId}/entries")
    @Operation(summary = "Record an AI-use disclosure entry (accepted/edited/rejected)")
    public EntryResponse append(@PathVariable UUID documentId, @Valid @RequestBody AppendRequest req) {
        return EntryResponse.from(service.append(documentId, req.documentSectionId(), req.aiRequestId(),
                req.featureKey(), req.model(), req.suggestionSummary(), req.action()));
    }
}
