package africa.credresearch.modules.paper.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.paper.domain.model.Paper;
import africa.credresearch.modules.paper.domain.port.PaperRepository;
import africa.credresearch.modules.paper.infrastructure.PaperExtractionClient;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uploaded papers → extracted metadata → formatted reference list (Phase 5, FR-LIT-1/2/7). Access
 * is gated on project membership; extraction is delegated to the worker (best-effort, degrades to
 * a manually-editable record).
 */
@Service
public class PaperService {

    /** A formatted reference entry for the reference-list view. */
    public record Reference(UUID paperId, String text) {}

    private final PaperRepository papers;
    private final PaperExtractionClient extraction;
    private final ProjectAccessGuard projectAccess;

    public PaperService(PaperRepository papers, PaperExtractionClient extraction, ProjectAccessGuard projectAccess) {
        this.papers = papers;
        this.extraction = extraction;
        this.projectAccess = projectAccess;
    }

    @Transactional
    public Paper upload(UUID projectId, String filename, byte[] bytes) {
        projectAccess.requireMember(projectId);
        if (bytes == null || bytes.length == 0) {
            throw ApiException.badRequest("EMPTY_FILE", "The uploaded file is empty");
        }
        UUID userId = TenantContextHolder.require().userId();
        JsonNode ex = extraction.extract(filename, bytes);
        boolean lowConfidence = ex.path("low_confidence").asBoolean(false);
        Paper meta = new Paper(
                null, projectId, userId, filename,
                text(ex, "title"), text(ex, "authors"), intOrNull(ex, "year"),
                text(ex, "doi"), text(ex, "journal"),
                lowConfidence ? "LOW_CONFIDENCE" : "DONE", null);
        return papers.create(meta, ex.path("text").asText(""));
    }

    public List<Paper> list(UUID projectId) {
        projectAccess.requireMember(projectId);
        return papers.findByProject(projectId);
    }

    @Transactional
    public Paper updateMetadata(UUID id, String title, String authors, Integer year, String doi, String journal) {
        require(id);
        return papers.updateMetadata(id, title, authors, year, doi, journal);
    }

    @Transactional
    public void delete(UUID id) {
        require(id);
        papers.delete(id);
    }

    public List<Reference> references(UUID projectId, String style) {
        List<Paper> list = list(projectId);
        return list.stream()
                .sorted((a, b) -> sortKey(a).compareToIgnoreCase(sortKey(b)))
                .map(p -> new Reference(p.id(), ReferenceFormatter.format(p, style)))
                .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private Paper require(UUID id) {
        Paper p = papers.findById(id)
                .orElseThrow(() -> ApiException.notFound("PAPER_NOT_FOUND", "Paper not found"));
        projectAccess.requireMember(p.projectId());
        return p;
    }

    private static String sortKey(Paper p) {
        if (p.authors() != null && !p.authors().isBlank()) return p.authors();
        if (p.title() != null && !p.title().isBlank()) return p.title();
        return p.filename() == null ? "" : p.filename();
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isInt() || v.isLong() ? v.asInt() : null;
    }
}
