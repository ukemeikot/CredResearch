package africa.credresearch.modules.document.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.disclosure.application.DisclosureService;
import africa.credresearch.modules.disclosure.infrastructure.persistence.entity.DisclosureEntryEntity;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.paper.application.PaperService;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/**
 * Submission bundle (Phase 10, FR-DOC-8 + FR-LEDGER-3): a single ZIP containing the document as
 * DOCX (+ PDF when available), the formatted reference list, and an **AI-Use disclosure statement**
 * generated from the document's disclosure ledger. This is the "document leaves the building with a
 * disclosure statement" deliverable.
 */
@Service
public class BundleService {

    public record BundleResult(byte[] bytes, String filename) {}

    private final DocumentExportService exportService;
    private final DisclosureService disclosure;
    private final PaperService papers;
    private final DocumentRepository documents;

    public BundleService(DocumentExportService exportService, DisclosureService disclosure,
                         PaperService papers, DocumentRepository documents) {
        this.exportService = exportService;
        this.disclosure = disclosure;
        this.papers = papers;
        this.documents = documents;
    }

    public BundleResult bundle(UUID documentId) {
        // DOCX export also enforces membership access.
        DocumentExportService.ExportResult docx = exportService.export(documentId, "docx");
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));

        byte[] pdf = null;
        try {
            pdf = exportService.export(documentId, "pdf").bytes();
        } catch (RuntimeException ignored) {
            // PDF conversion not available (no Gotenberg) — bundle DOCX-only rather than failing.
        }

        String base = safeName(doc.title());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            put(zip, base + ".docx", docx.bytes());
            if (pdf != null) {
                put(zip, base + ".pdf", pdf);
            }
            put(zip, "references.txt", referencesText(doc.projectId()).getBytes(StandardCharsets.UTF_8));
            put(zip, "ai-use-disclosure.txt", disclosureStatement(documentId, doc.title()).getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw ApiException.of(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "BUNDLE_FAILED", "Could not assemble the bundle");
        }
        return new BundleResult(out.toByteArray(), base + "-submission.zip");
    }

    private String referencesText(UUID projectId) {
        List<PaperService.Reference> refs = papers.references(projectId, "APA");
        if (refs.isEmpty()) {
            return "References\n=========\n(No references added yet.)\n";
        }
        StringBuilder sb = new StringBuilder("References (APA)\n================\n\n");
        int i = 1;
        for (PaperService.Reference r : refs) {
            sb.append(i++).append(". ").append(r.text()).append("\n\n");
        }
        return sb.toString();
    }

    /** Human-readable AI-Use disclosure statement from the hash-chained ledger (FR-LEDGER-3). */
    private String disclosureStatement(UUID documentId, String title) {
        List<DisclosureEntryEntity> entries = disclosure.list(documentId);
        StringBuilder sb = new StringBuilder();
        sb.append("AI-USE DISCLOSURE STATEMENT\n===========================\n\n");
        sb.append("Document: ").append(title == null ? "Untitled" : title).append("\n\n");
        if (entries.isEmpty()) {
            sb.append("No AI assistance was recorded for this document.\n");
            return sb.toString();
        }
        sb.append("The following AI assistance was used while preparing this document. This record is\n");
        sb.append("append-only and tamper-evident (each entry is hash-chained to the previous one).\n\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(java.time.ZoneOffset.UTC);
        int i = 1;
        for (DisclosureEntryEntity e : entries) {
            sb.append(i++).append(". ").append(fmt.format(e.getCreatedAt()))
                    .append(" — ").append(e.getFeatureKey())
                    .append(" (").append(e.getAction()).append(")");
            if (e.getModel() != null) sb.append(", model: ").append(e.getModel());
            sb.append("\n");
            if (e.getSuggestionSummary() != null && !e.getSuggestionSummary().isBlank()) {
                sb.append("   \"").append(e.getSuggestionSummary()).append("\"\n");
            }
            sb.append("   ledger hash: ").append(e.getEntryHash()).append("\n\n");
        }
        return sb.toString();
    }

    private void put(ZipOutputStream zip, String name, byte[] data) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private String safeName(String title) {
        String base = title == null || title.isBlank() ? "document" : title;
        String safe = base.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "_");
        return safe.isBlank() ? "document" : safe;
    }
}
