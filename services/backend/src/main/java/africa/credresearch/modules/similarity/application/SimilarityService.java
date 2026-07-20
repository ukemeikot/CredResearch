package africa.credresearch.modules.similarity.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.document.domain.port.DocumentSectionRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import africa.credresearch.modules.similarity.infrastructure.SimilarityClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Internal similarity pre-check (Phase 9, FR-SIM-1/3/4/5): compares a document against the OTHER
 * documents in the same project (the user's own corpus), flagging repeated paragraphs and
 * citation risk. Deterministic (worker shingle/Jaccard) — explicitly NOT a Turnitin equivalent.
 */
@Service
public class SimilarityService {

    private final DocumentRepository documents;
    private final DocumentSectionRepository sections;
    private final ProjectAccessGuard projectAccess;
    private final SimilarityClient client;
    private final ObjectMapper mapper;

    public SimilarityService(DocumentRepository documents, DocumentSectionRepository sections,
                             ProjectAccessGuard projectAccess, SimilarityClient client, ObjectMapper mapper) {
        this.documents = documents;
        this.sections = sections;
        this.projectAccess = projectAccess;
        this.client = client;
        this.mapper = mapper;
    }

    public JsonNode check(UUID documentId) {
        Document target = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));
        projectAccess.requireMember(target.projectId());

        String targetText = gatherText(documentId);
        if (targetText.isBlank()) {
            throw ApiException.badRequest("NO_TEXT", "This document has no text to check yet.");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("target_text", targetText);
        ArrayNode srcArr = body.putArray("sources");
        for (Document other : documents.findByProject(target.projectId())) {
            if (other.id().equals(documentId)) {
                continue; // the target isn't a source for itself
            }
            String text = gatherText(other.id());
            if (!text.isBlank()) {
                ObjectNode s = srcArr.addObject();
                s.put("id", other.id().toString());
                s.put("title", other.title());
                s.put("text", text);
            }
        }
        return client.check(body);
    }

    /** Flatten a document's sections into paragraph text (blank line between sections). */
    private String gatherText(UUID documentId) {
        StringBuilder sb = new StringBuilder();
        for (DocumentSection s : sections.findByDocument(documentId)) {
            if (s.contentText() != null && !s.contentText().isBlank()) {
                sb.append(s.contentText().strip()).append("\n\n");
            }
        }
        return sb.toString().strip();
    }
}
