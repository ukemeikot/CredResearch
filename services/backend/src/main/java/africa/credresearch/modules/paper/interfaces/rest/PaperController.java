package africa.credresearch.modules.paper.interfaces.rest;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.paper.application.PaperService;
import africa.credresearch.modules.paper.domain.model.Paper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/papers")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Papers", description = "Uploaded source papers, extracted metadata, and reference-list "
        + "rendering (Phase 5, FR-LIT-1/2/7).")
public class PaperController {

    private final PaperService service;
    private final ObjectMapper mapper;

    public PaperController(PaperService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    public record PaperResponse(UUID id, UUID projectId, String filename, String title, String authors,
                                Integer year, String doi, String journal, String extractionStatus,
                                JsonNode summary) {}

    private PaperResponse toResponse(Paper p) {
        JsonNode summary = null;
        if (p.summaryJson() != null) {
            try {
                summary = mapper.readTree(p.summaryJson());
            } catch (Exception ignored) {
                // stored summary unparseable → omit it rather than fail the whole response
            }
        }
        return new PaperResponse(p.id(), p.projectId(), p.filename(), p.title(), p.authors(),
                p.year(), p.doi(), p.journal(), p.extractionStatus(), summary);
    }

    public record UpdatePaperRequest(String title, String authors, Integer year, String doi, String journal) {}

    public record ReferenceResponse(UUID paperId, String text) {}

    public record ReferenceListResponse(String style, List<ReferenceResponse> references) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a paper (PDF/DOCX); extracts text + metadata")
    public PaperResponse upload(@RequestParam("projectId") UUID projectId,
                                @RequestParam("file") MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw ApiException.badRequest("UPLOAD_READ_FAILED", "Could not read the uploaded file");
        }
        Paper p = service.upload(projectId, file.getOriginalFilename(), bytes);
        // Index for RAG (best-effort; never fails the upload).
        service.indexPaperQuietly(p.id(), p.projectId());
        return toResponse(p);
    }

    public record AskRequest(UUID projectId, String question) {}

    @PostMapping("/ask")
    @Operation(summary = "Ask a question answered from the project's uploaded papers (RAG, FR-LIT-8)")
    public JsonNode ask(@RequestBody AskRequest req) {
        return service.ask(req.projectId(), req.question());
    }

    @GetMapping
    @Operation(summary = "List a project's uploaded papers")
    public List<PaperResponse> list(@RequestParam UUID projectId) {
        return service.list(projectId).stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Correct a paper's extracted metadata")
    public PaperResponse update(@PathVariable UUID id, @RequestBody UpdatePaperRequest req) {
        return toResponse(
                service.updateMetadata(id, req.title(), req.authors(), req.year(), req.doi(), req.journal()));
    }

    @PostMapping("/{id}/summarize")
    @Operation(summary = "AI-summarize a paper: methodology, findings, limitations, gaps (FR-LIT-4)")
    public JsonNode summarize(@PathVariable UUID id) {
        return service.summarize(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a paper")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/export")
    @Operation(summary = "Export the project's references as BibTeX or RIS (FR-LIT-9)")
    public org.springframework.http.ResponseEntity<byte[]> export(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "bibtex") String format) {
        String content = service.export(projectId, format);
        String ext = "ris".equalsIgnoreCase(format) ? "ris" : "bib";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("references." + ext).build().toString())
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/references")
    @Operation(summary = "Render the project's reference list in a citation style (APA/IEEE/HARVARD)")
    public ReferenceListResponse references(@RequestParam UUID projectId,
                                            @RequestParam(defaultValue = "APA") String style) {
        List<ReferenceResponse> refs = service.references(projectId, style).stream()
                .map(r -> new ReferenceResponse(r.paperId(), r.text()))
                .toList();
        return new ReferenceListResponse(style.toUpperCase(), refs);
    }
}
