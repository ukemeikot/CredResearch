package africa.credresearch.modules.document.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.model.DocumentDetail;
import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.model.DocumentVersion;
import africa.credresearch.modules.document.domain.model.TemplateDetail;
import africa.credresearch.modules.document.domain.model.TemplateSection;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.document.domain.port.DocumentSectionRepository;
import africa.credresearch.modules.document.domain.port.DocumentVersionRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents instantiated from templates, per-section rich-text editing with optimistic-lock
 * autosave and version history (FR-DOC-1..4). Access is delegated to the project's membership
 * guard: reads need membership; writes (create/autosave/restore) need the OWNER project-role.
 */
@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final DocumentSectionRepository sections;
    private final DocumentVersionRepository versions;
    private final TemplateService templates;
    private final ProjectAccessGuard projectAccess;

    public DocumentService(DocumentRepository documents, DocumentSectionRepository sections,
                           DocumentVersionRepository versions, TemplateService templates,
                           ProjectAccessGuard projectAccess) {
        this.documents = documents;
        this.sections = sections;
        this.versions = versions;
        this.templates = templates;
        this.projectAccess = projectAccess;
    }

    /** Creates a document from a template, instantiating an (empty) editable section per template section. */
    @Transactional
    public DocumentDetail createFromTemplate(UUID projectId, UUID templateId, String title) {
        projectAccess.requireRole(projectId, ProjectMemberRole.OWNER);
        TenantContext ctx = TenantContextHolder.require();
        TemplateDetail template = templates.get(templateId); // enforces visibility
        Document doc = documents.create(
                new Document(null, projectId, templateId,
                        title == null || title.isBlank() ? template.template().name() : title, "DRAFT"),
                ctx.userId());
        List<DocumentSection> seeded = template.sections().stream()
                .map((TemplateSection s) -> new DocumentSection(null, doc.id(), s.orderIndex(),
                        s.chapter(), s.heading(), null, null, 1))
                .toList();
        sections.createAll(seeded);
        return new DocumentDetail(doc, sections.findByDocument(doc.id()));
    }

    public List<Document> listByProject(UUID projectId) {
        projectAccess.requireMember(projectId);
        return documents.findByProject(projectId);
    }

    // ── Section structure editing (add / rename / reorder / delete) — owner-only ──
    /** Adds a new (empty) section, appended at the end of the document. */
    @Transactional
    public DocumentSection addSection(UUID documentId, String heading, String chapter) {
        Document doc = requireDocumentOwner(documentId);
        if (heading == null || heading.isBlank()) {
            throw ApiException.badRequest("INVALID_HEADING", "A section heading is required");
        }
        int next = sections.maxOrderIndex(doc.id()) + 1;
        return sections.add(new DocumentSection(null, doc.id(), next, chapter, heading, null, null, 1));
    }

    /** Renames / re-chapters / moves a section (structural metadata only, not content). */
    @Transactional
    public DocumentSection updateSection(UUID documentId, UUID sectionId, String heading,
                                         String chapter, Integer orderIndex) {
        Document doc = requireDocumentOwner(documentId);
        requireSection(doc.id(), sectionId);
        if (heading != null && heading.isBlank()) {
            throw ApiException.badRequest("INVALID_HEADING", "A section heading cannot be blank");
        }
        sections.updateMeta(sectionId, heading, chapter, orderIndex);
        return sections.findById(sectionId).orElseThrow();
    }

    /** Deletes a section and its version history. */
    @Transactional
    public void deleteSection(UUID documentId, UUID sectionId) {
        Document doc = requireDocumentOwner(documentId);
        requireSection(doc.id(), sectionId);
        versions.deleteBySection(sectionId);
        sections.delete(sectionId);
    }

    public DocumentDetail get(UUID documentId) {
        Document doc = requireDocumentMember(documentId);
        return new DocumentDetail(doc, sections.findByDocument(documentId));
    }

    public DocumentSection getSection(UUID documentId, UUID sectionId) {
        requireDocumentMember(documentId);
        return requireSection(documentId, sectionId);
    }

    /** Autosave a section with optimistic locking (FR-DOC-3). 409 on a version conflict. */
    @Transactional
    public DocumentSection autosave(UUID documentId, UUID sectionId, JsonNode content, int expectedVersion) {
        Document doc = requireDocumentOwner(documentId);
        DocumentSection section = requireSection(doc.id(), sectionId);
        String json = (content == null || content.isNull()) ? null : content.toString();
        String text = ProseMirrorText.flatten(content);
        DocumentSection saved = sections.tryAutosave(sectionId, expectedVersion, json, text,
                        TenantContextHolder.require().userId())
                .orElseThrow(() -> ApiException.conflict("VERSION_CONFLICT",
                        "This section changed since you opened it. Reload to see the latest version."));
        versions.snapshot(sectionId, saved.version(), json, text, TenantContextHolder.require().userId());
        return saved;
    }

    public List<DocumentVersion> listVersions(UUID documentId, UUID sectionId) {
        requireDocumentMember(documentId);
        requireSection(documentId, sectionId);
        return versions.findBySection(sectionId);
    }

    /** Restores a prior version's content as a new current version (FR-DOC-4). */
    @Transactional
    public DocumentSection restore(UUID documentId, UUID sectionId, UUID versionId) {
        Document doc = requireDocumentOwner(documentId);
        DocumentSection current = requireSection(doc.id(), sectionId);
        DocumentVersion version = versions.findById(versionId)
                .orElseThrow(() -> ApiException.notFound("VERSION_NOT_FOUND", "Version not found"));
        if (!version.documentSectionId().equals(sectionId)) {
            throw ApiException.badRequest("VERSION_MISMATCH", "Version does not belong to this section");
        }
        UUID userId = TenantContextHolder.require().userId();
        // Reuse the optimistic-lock path so a restore that races an autosave fails cleanly (409)
        // instead of colliding on the (section, version) unique constraint.
        DocumentSection restored = sections
                .tryAutosave(sectionId, current.version(), version.content(), version.contentText(), userId)
                .orElseThrow(() -> ApiException.conflict("VERSION_CONFLICT",
                        "This section changed while restoring. Reopen it and try again."));
        versions.snapshot(sectionId, restored.version(), version.content(), version.contentText(), userId);
        return restored;
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private Document requireDocumentMember(UUID documentId) {
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));
        projectAccess.requireMember(doc.projectId()); // also blocks cross-tenant (project resolves in tenant)
        return doc;
    }

    private Document requireDocumentOwner(UUID documentId) {
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document not found"));
        projectAccess.requireRole(doc.projectId(), ProjectMemberRole.OWNER);
        return doc;
    }

    private DocumentSection requireSection(UUID documentId, UUID sectionId) {
        DocumentSection s = sections.findById(sectionId)
                .orElseThrow(() -> ApiException.notFound("SECTION_NOT_FOUND", "Section not found"));
        if (!s.documentId().equals(documentId)) {
            throw ApiException.notFound("SECTION_NOT_FOUND", "Section not found in this document");
        }
        return s;
    }
}
