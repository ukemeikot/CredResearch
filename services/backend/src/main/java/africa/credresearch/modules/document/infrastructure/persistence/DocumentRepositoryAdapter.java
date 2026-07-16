package africa.credresearch.modules.document.infrastructure.persistence;

import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.document.infrastructure.persistence.entity.DocumentEntity;
import africa.credresearch.modules.document.infrastructure.persistence.repository.DocumentJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpa;

    public DocumentRepositoryAdapter(DocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Document create(Document d, UUID createdBy) {
        DocumentEntity e = new DocumentEntity();
        e.setProjectId(d.projectId());
        e.setTemplateId(d.templateId());
        e.setTitle(d.title());
        e.setStatus(d.status() == null ? "DRAFT" : d.status());
        e.setCreatedBy(createdBy);
        e.setUpdatedBy(createdBy);
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(DocumentRepositoryAdapter::toDomain);
    }

    @Override
    public List<Document> findByProject(UUID projectId) {
        return jpa.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId).stream()
                .map(DocumentRepositoryAdapter::toDomain).toList();
    }

    static Document toDomain(DocumentEntity e) {
        return new Document(e.getId(), e.getProjectId(), e.getTemplateId(), e.getTitle(), e.getStatus());
    }
}
