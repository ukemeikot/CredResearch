package africa.credresearch.modules.document.infrastructure.persistence.repository;

import africa.credresearch.modules.document.infrastructure.persistence.entity.DocumentVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, UUID> {
    List<DocumentVersionEntity> findByDocumentSectionIdOrderByVersionDesc(UUID sectionId);
    void deleteByDocumentSectionId(UUID sectionId);
}
