package africa.credresearch.modules.document.domain.port;

import africa.credresearch.modules.document.domain.model.DocumentVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository {

    void snapshot(UUID sectionId, int version, String content, String contentText, UUID authoredBy);

    List<DocumentVersion> findBySection(UUID sectionId);

    Optional<DocumentVersion> findById(UUID id);

    /** Removes all version history for a section (called before deleting the section). */
    void deleteBySection(UUID sectionId);
}
