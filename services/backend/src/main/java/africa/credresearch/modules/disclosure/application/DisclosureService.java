package africa.credresearch.modules.disclosure.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.common.util.TokenHasher;
import africa.credresearch.modules.disclosure.infrastructure.persistence.entity.DisclosureEntryEntity;
import africa.credresearch.modules.disclosure.infrastructure.persistence.repository.DisclosureEntryJpaRepository;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-Use Disclosure Ledger (FR-LEDGER): an append-only, hash-chained record of AI assistance used
 * in a document. Each entry's hash covers the previous entry's hash, so tampering is detectable.
 * Access follows the document's project membership.
 */
@Service
public class DisclosureService {

    private final DisclosureEntryJpaRepository entries;
    private final DocumentRepository documents;
    private final ProjectAccessGuard projectAccess;

    public DisclosureService(DisclosureEntryJpaRepository entries, DocumentRepository documents,
                             ProjectAccessGuard projectAccess) {
        this.entries = entries;
        this.documents = documents;
        this.projectAccess = projectAccess;
    }

    private Document requireMember(UUID documentId) {
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));
        projectAccess.requireMember(doc.projectId());
        return doc;
    }

    public List<DisclosureEntryEntity> list(UUID documentId) {
        requireMember(documentId);
        return entries.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    /** Appends a hash-chained disclosure entry recording an AI action on a document. */
    @Transactional
    public DisclosureEntryEntity append(UUID documentId, UUID sectionId, UUID aiRequestId,
                                        String featureKey, String model, String summary, String action) {
        requireMember(documentId);
        String prev = entries.findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .map(DisclosureEntryEntity::getEntryHash).orElse("");
        Instant now = Instant.now();
        String act = (action == null || action.isBlank()) ? "accepted" : action;
        String payload = String.join("|", nz(documentId), nz(sectionId), nz(featureKey), act,
                nz(summary), now.toString());
        String hash = TokenHasher.sha256(prev + "|" + payload);

        DisclosureEntryEntity e = new DisclosureEntryEntity();
        e.setDocumentId(documentId);
        e.setDocumentSectionId(sectionId);
        e.setAiRequestId(aiRequestId);
        e.setUserId(TenantContextHolder.require().userId());
        e.setFeatureKey(featureKey);
        e.setModel(model);
        e.setSuggestionSummary(summary == null ? null : summary.substring(0, Math.min(summary.length(), 500)));
        e.setAction(act);
        e.setPrevHash(prev);
        e.setEntryHash(hash);
        e.setCreatedAt(now);
        return entries.save(e);
    }

    private static String nz(Object o) {
        return o == null ? "" : o.toString();
    }
}
