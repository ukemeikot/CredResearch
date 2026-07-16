package africa.credresearch.modules.document.domain.port;

import africa.credresearch.modules.document.domain.model.DocumentSection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentSectionRepository {

    void createAll(List<DocumentSection> sections);

    /** Adds a single section and returns it (used for document-level section CRUD). */
    DocumentSection add(DocumentSection section);

    List<DocumentSection> findByDocument(UUID documentId);

    Optional<DocumentSection> findById(UUID id);

    /** The current max order_index for a document (-1 if none), so a new section appends at the end. */
    int maxOrderIndex(UUID documentId);

    /** Updates structural metadata (heading/chapter/order); null fields are left unchanged. */
    void updateMeta(UUID sectionId, String heading, String chapter, Integer orderIndex);

    /** Deletes a section (its version history is removed first by the service). */
    void delete(UUID sectionId);

    /**
     * Compare-and-set autosave (FR-DOC-3). Updates the section only if its current version equals
     * {@code expectedVersion}, bumping it to {@code expectedVersion + 1}. Returns the updated
     * section, or empty on a version conflict (caller surfaces 409).
     */
    Optional<DocumentSection> tryAutosave(
            UUID sectionId, int expectedVersion, String content, String contentText, UUID updatedBy);
}
