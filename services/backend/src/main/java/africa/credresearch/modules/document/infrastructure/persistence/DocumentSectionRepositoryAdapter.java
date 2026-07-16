package africa.credresearch.modules.document.infrastructure.persistence;

import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.port.DocumentSectionRepository;
import africa.credresearch.modules.document.infrastructure.persistence.entity.DocumentSectionEntity;
import africa.credresearch.modules.document.infrastructure.persistence.repository.DocumentSectionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DocumentSectionRepositoryAdapter implements DocumentSectionRepository {

    private final DocumentSectionJpaRepository jpa;

    public DocumentSectionRepositoryAdapter(DocumentSectionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void createAll(List<DocumentSection> list) {
        List<DocumentSectionEntity> entities = list.stream().map(s -> {
            DocumentSectionEntity e = new DocumentSectionEntity();
            e.setDocumentId(s.documentId());
            e.setOrderIndex(s.orderIndex());
            e.setChapter(s.chapter());
            e.setHeading(s.heading());
            e.setContent(s.content());
            e.setContentText(s.contentText());
            e.setVersion(s.version() <= 0 ? 1 : s.version());
            return e;
        }).toList();
        jpa.saveAll(entities);
    }

    @Override
    public List<DocumentSection> findByDocument(UUID documentId) {
        return jpa.findByDocumentIdOrderByOrderIndexAsc(documentId).stream()
                .map(DocumentSectionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<DocumentSection> findById(UUID id) {
        return jpa.findById(id).map(DocumentSectionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<DocumentSection> tryAutosave(UUID sectionId, int expectedVersion,
                                                 String content, String contentText, UUID updatedBy) {
        int updated = jpa.autosave(sectionId, expectedVersion, content, contentText, updatedBy);
        if (updated == 0) {
            return Optional.empty();
        }
        return jpa.findById(sectionId).map(DocumentSectionRepositoryAdapter::toDomain);
    }

    static DocumentSection toDomain(DocumentSectionEntity e) {
        return new DocumentSection(e.getId(), e.getDocumentId(), e.getOrderIndex(), e.getChapter(),
                e.getHeading(), e.getContent(), e.getContentText(), e.getVersion());
    }
}
