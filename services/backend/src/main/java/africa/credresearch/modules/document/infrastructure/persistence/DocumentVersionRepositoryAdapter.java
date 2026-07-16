package africa.credresearch.modules.document.infrastructure.persistence;

import africa.credresearch.modules.document.domain.model.DocumentVersion;
import africa.credresearch.modules.document.domain.port.DocumentVersionRepository;
import africa.credresearch.modules.document.infrastructure.persistence.entity.DocumentVersionEntity;
import africa.credresearch.modules.document.infrastructure.persistence.repository.DocumentVersionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentVersionRepositoryAdapter implements DocumentVersionRepository {

    private final DocumentVersionJpaRepository jpa;

    public DocumentVersionRepositoryAdapter(DocumentVersionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void snapshot(UUID sectionId, int version, String content, String contentText, UUID authoredBy) {
        DocumentVersionEntity e = new DocumentVersionEntity();
        e.setDocumentSectionId(sectionId);
        e.setVersion(version);
        e.setContent(content);
        e.setContentText(contentText);
        e.setAuthoredBy(authoredBy);
        jpa.save(e);
    }

    @Override
    public List<DocumentVersion> findBySection(UUID sectionId) {
        return jpa.findByDocumentSectionIdOrderByVersionDesc(sectionId).stream()
                .map(DocumentVersionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<DocumentVersion> findById(UUID id) {
        return jpa.findById(id).map(DocumentVersionRepositoryAdapter::toDomain);
    }

    static DocumentVersion toDomain(DocumentVersionEntity e) {
        return new DocumentVersion(e.getId(), e.getDocumentSectionId(), e.getVersion(),
                e.getContent(), e.getContentText(), e.getAuthoredBy(), e.getCreatedAt());
    }
}
