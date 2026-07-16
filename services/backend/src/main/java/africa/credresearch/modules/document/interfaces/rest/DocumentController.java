package africa.credresearch.modules.document.interfaces.rest;

import africa.credresearch.modules.document.application.DocumentService;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.model.DocumentDetail;
import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.model.DocumentVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Documents", description = "Documents instantiated from templates; per-section rich-text "
        + "editing with optimistic-lock autosave and version history. FR-DOC-1..4.")
public class DocumentController {

    private final DocumentService service;
    private final ObjectMapper objectMapper;

    public DocumentController(DocumentService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public record DocumentResponse(UUID id, UUID projectId, UUID templateId, String title, String status) {
        static DocumentResponse from(Document d) {
            return new DocumentResponse(d.id(), d.projectId(), d.templateId(), d.title(), d.status());
        }
    }

    public record SectionResponse(UUID id, UUID documentId, int orderIndex, String chapter, String heading,
                                  JsonNode content, int version) {}

    public record DocumentDetailResponse(DocumentResponse document, List<SectionResponse> sections) {}

    public record VersionResponse(UUID id, int version, UUID authoredBy, Instant createdAt) {}

    public record CreateDocumentRequest(@NotNull UUID projectId, @NotNull UUID templateId, String title) {}

    public record AutosaveRequest(JsonNode content, @NotNull Integer version) {}

    public record RestoreRequest(@NotNull UUID versionId) {}

    // ── Endpoints ──────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "List a project's documents")
    public List<DocumentResponse> listByProject(@RequestParam UUID projectId) {
        return service.listByProject(projectId).stream().map(DocumentResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Create a document from a template (FR-DOC-1)")
    public DocumentDetailResponse create(@Valid @RequestBody CreateDocumentRequest req) {
        return toDetail(service.createFromTemplate(req.projectId(), req.templateId(), req.title()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document with its sections")
    public DocumentDetailResponse get(@PathVariable UUID id) {
        return toDetail(service.get(id));
    }

    @GetMapping("/{id}/sections/{sectionId}")
    @Operation(summary = "Get one section")
    public SectionResponse getSection(@PathVariable UUID id, @PathVariable UUID sectionId) {
        return toSection(service.getSection(id, sectionId));
    }

    @PutMapping("/{id}/sections/{sectionId}")
    @Operation(summary = "Autosave a section (optimistic lock via version)",
            description = "Returns the section with its new version. 409 if the stored version has moved on.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved"),
            @ApiResponse(responseCode = "409", description = "Version conflict — reload", content = @Content())
    })
    public SectionResponse autosave(@PathVariable UUID id, @PathVariable UUID sectionId,
                                    @Valid @RequestBody AutosaveRequest req) {
        return toSection(service.autosave(id, sectionId, req.content(), req.version()));
    }

    @GetMapping("/{id}/sections/{sectionId}/versions")
    @Operation(summary = "Section version history (FR-DOC-4)")
    public List<VersionResponse> versions(@PathVariable UUID id, @PathVariable UUID sectionId) {
        return service.listVersions(id, sectionId).stream()
                .map(v -> new VersionResponse(v.id(), v.version(), v.authoredBy(), v.createdAt()))
                .toList();
    }

    @PostMapping("/{id}/sections/{sectionId}/restore")
    @Operation(summary = "Restore a prior version (FR-DOC-4)")
    public SectionResponse restore(@PathVariable UUID id, @PathVariable UUID sectionId,
                                   @Valid @RequestBody RestoreRequest req) {
        return toSection(service.restore(id, sectionId, req.versionId()));
    }

    // ── mapping ──────────────────────────────────────────────────────────────
    private DocumentDetailResponse toDetail(DocumentDetail d) {
        return new DocumentDetailResponse(DocumentResponse.from(d.document()),
                d.sections().stream().map(this::toSection).toList());
    }

    private SectionResponse toSection(DocumentSection s) {
        return new SectionResponse(s.id(), s.documentId(), s.orderIndex(), s.chapter(), s.heading(),
                parse(s.content()), s.version());
    }

    private JsonNode parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
