package africa.credresearch.modules.document.domain.port;

import africa.credresearch.modules.document.domain.model.DocumentSection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentSectionRepository {

    void createAll(List<DocumentSection> sections);

    List<DocumentSection> findByDocument(UUID documentId);

    Optional<DocumentSection> findById(UUID id);

    /**
     * Compare-and-set autosave (FR-DOC-3). Updates the section only if its current version equals
     * {@code expectedVersion}, bumping it to {@code expectedVersion + 1}. Returns the updated
     * section, or empty on a version conflict (caller surfaces 409).
     */
    Optional<DocumentSection> tryAutosave(
            UUID sectionId, int expectedVersion, String content, String contentText, UUID updatedBy);
}
