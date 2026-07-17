package africa.credresearch.modules.document.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.model.DocumentDetail;
import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.model.FormatRule;
import africa.credresearch.modules.document.domain.model.TemplateDetail;
import africa.credresearch.modules.document.infrastructure.DocumentExportClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Renders a document to a downloadable file (DOCX now; PDF when a conversion service is wired) by
 * gathering its ordered sections + the template's format rule and delegating to the worker
 * (FR-DOC-6/7). Access reuses the document read guard (project membership).
 */
@Service
public class DocumentExportService {

    /** A rendered file: its bytes, MIME type, and a suggested download filename. */
    public record ExportResult(byte[] bytes, String contentType, String filename) {}

    private final DocumentService documents;
    private final TemplateService templates;
    private final DocumentExportClient client;
    private final ObjectMapper mapper;

    public DocumentExportService(DocumentService documents, TemplateService templates,
                                 DocumentExportClient client, ObjectMapper mapper) {
        this.documents = documents;
        this.templates = templates;
        this.client = client;
        this.mapper = mapper;
    }

    public ExportResult export(UUID documentId, String format) {
        String fmt = format == null ? "docx" : format.toLowerCase();
        if (!fmt.equals("docx") && !fmt.equals("pdf")) {
            throw ApiException.badRequest("UNSUPPORTED_FORMAT", "Supported formats: docx, pdf");
        }
        DocumentDetail detail = documents.get(documentId); // enforces membership
        FormatRule rule = templates.get(detail.document().templateId()).formatRule();

        byte[] bytes = client.render(fmt, buildPayload(detail, rule));
        String contentType = fmt.equals("pdf")
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return new ExportResult(bytes, contentType, filename(detail.document(), fmt));
    }

    private ObjectNode buildPayload(DocumentDetail detail, FormatRule rule) {
        ObjectNode root = mapper.createObjectNode();
        root.put("title", detail.document().title());
        ObjectNode fr = root.putObject("format_rule");
        fr.put("font_family", rule != null && rule.fontFamily() != null ? rule.fontFamily() : "Times New Roman");
        fr.put("font_size_pt", rule != null && rule.fontSizePt() != null ? rule.fontSizePt().doubleValue() : 12.0);
        fr.put("line_spacing", rule != null && rule.lineSpacing() != null ? rule.lineSpacing().doubleValue() : 2.0);

        ArrayNode arr = root.putArray("sections");
        for (DocumentSection s : detail.sections()) {
            ObjectNode node = arr.addObject();
            node.put("chapter", s.chapter());
            node.put("heading", s.heading());
            if (s.content() != null && !s.content().isBlank()) {
                try {
                    node.set("content", mapper.readTree(s.content()));
                } catch (Exception ignored) {
                    // A section with unparseable content is exported empty rather than failing the whole file.
                }
            }
        }
        return root;
    }

    private String filename(Document doc, String fmt) {
        String base = doc.title() == null || doc.title().isBlank() ? "document" : doc.title();
        String safe = base.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "_");
        if (safe.isBlank()) {
            safe = "document";
        }
        return safe + "." + fmt;
    }
}
