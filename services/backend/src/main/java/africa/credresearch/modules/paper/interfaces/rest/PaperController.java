package africa.credresearch.modules.paper.interfaces.rest;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.paper.application.PaperService;
import africa.credresearch.modules.paper.domain.model.Paper;
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

    public PaperController(PaperService service) {
        this.service = service;
    }

    public record PaperResponse(UUID id, UUID projectId, String filename, String title, String authors,
                                Integer year, String doi, String journal, String extractionStatus) {
        static PaperResponse from(Paper p) {
            return new PaperResponse(p.id(), p.projectId(), p.filename(), p.title(), p.authors(),
                    p.year(), p.doi(), p.journal(), p.extractionStatus());
        }
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
        return PaperResponse.from(service.upload(projectId, file.getOriginalFilename(), bytes));
    }

    @GetMapping
    @Operation(summary = "List a project's uploaded papers")
    public List<PaperResponse> list(@RequestParam UUID projectId) {
        return service.list(projectId).stream().map(PaperResponse::from).toList();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Correct a paper's extracted metadata")
    public PaperResponse update(@PathVariable UUID id, @RequestBody UpdatePaperRequest req) {
        return PaperResponse.from(
                service.updateMetadata(id, req.title(), req.authors(), req.year(), req.doi(), req.journal()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a paper")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
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
