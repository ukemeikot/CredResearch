package africa.credresearch.modules.paper.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.ai.application.AiUsageService;
import africa.credresearch.modules.ai.infrastructure.AiWorkerClient;
import africa.credresearch.modules.identity.application.ProfileService;
import africa.credresearch.modules.paper.domain.model.Paper;
import africa.credresearch.modules.paper.domain.port.PaperRepository;
import africa.credresearch.modules.paper.infrastructure.PaperExtractionClient;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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
    private final AiUsageService usage;
    private final ProfileService profiles;
    private final AiWorkerClient worker;
    private final ObjectMapper mapper;
    private final String modelLabel;

    public PaperService(PaperRepository papers, PaperExtractionClient extraction, ProjectAccessGuard projectAccess,
                        AiUsageService usage, ProfileService profiles, AiWorkerClient worker, ObjectMapper mapper,
                        @Value("${credresearch.ai.model-label:self-hosted}") String modelLabel) {
        this.papers = papers;
        this.extraction = extraction;
        this.projectAccess = projectAccess;
        this.usage = usage;
        this.profiles = profiles;
        this.worker = worker;
        this.mapper = mapper;
        this.modelLabel = modelLabel;
    }

    /**
     * AI-summarize a paper (method/findings/limitations/gaps) — FR-LIT-4. Credit-metered and
     * recorded like other AI features; requires a verified email. Stores the summary on the paper.
     */
    @Transactional
    public JsonNode summarize(UUID id) {
        Paper paper = require(id);
        if (!profiles.currentUser().isEmailVerified()) {
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED",
                    "Please verify your email before using AI features.");
        }
        TenantContext ctx = TenantContextHolder.require();
        usage.assertWithinCredits(ctx.userId(), ctx.plan());
        String text = papers.getText(id).orElse("");
        if (text.isBlank()) {
            throw ApiException.badRequest("NO_TEXT", "This paper has no extracted text to summarize.");
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        body.put("projectId", paper.projectId().toString());
        UUID requestId = usage.recordRequest(ctx.institutionId(), paper.projectId(), null,
                ctx.userId(), "summarize", modelLabel);
        try {
            JsonNode resp = worker.post("summarize", body);
            usage.recordResponse(requestId, resp == null ? null : resp.toString(), "stop");
            papers.saveSummary(id, resp == null ? null : resp.toString());
            return resp;
        } catch (RuntimeException e) {
            usage.markError(requestId);
            throw e;
        }
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
        String doi = text(ex, "doi");
        String title = text(ex, "title");

        // Deduplicate within the project by DOI, else by normalised title (FR-LIT-10).
        List<Paper> existing = papers.findByProject(projectId);
        boolean dup = existing.stream().anyMatch(p ->
                (doi != null && doi.equalsIgnoreCase(p.doi()))
                        || (title != null && normalize(title).equals(normalize(p.title()))));
        if (dup) {
            throw ApiException.conflict("DUPLICATE_PAPER",
                    "This paper is already in the project (matched by DOI or title).");
        }

        Paper meta = new Paper(
                null, projectId, userId, filename,
                title, text(ex, "authors"), intOrNull(ex, "year"),
                doi, text(ex, "journal"),
                lowConfidence ? "LOW_CONFIDENCE" : "DONE", null, null);
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

    /** Serialise the project's papers to BibTeX or RIS for reference-manager import (FR-LIT-9). */
    public String export(UUID projectId, String format) {
        List<Paper> list = list(projectId);
        return "ris".equalsIgnoreCase(format)
                ? BibliographyExporter.toRis(list)
                : BibliographyExporter.toBibtex(list);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

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
