package africa.credresearch.modules.paper.domain.port;

import africa.credresearch.modules.paper.domain.model.Paper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaperRepository {

    Paper create(Paper paper, String textContent);

    List<Paper> findByProject(UUID projectId);

    Optional<Paper> findById(UUID id);

    Paper updateMetadata(UUID id, String title, String authors, Integer year, String doi, String journal);

    /** The extracted full text, for summarization/RAG (not returned in list responses). */
    Optional<String> getText(UUID id);

    Paper saveSummary(UUID id, String summaryJson);

    void delete(UUID id);
}
