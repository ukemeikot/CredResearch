package africa.credresearch.modules.document.domain.port;

import africa.credresearch.modules.document.domain.model.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    Document create(Document document, UUID createdBy);

    Optional<Document> findById(UUID id);

    List<Document> findByProject(UUID projectId);
}
