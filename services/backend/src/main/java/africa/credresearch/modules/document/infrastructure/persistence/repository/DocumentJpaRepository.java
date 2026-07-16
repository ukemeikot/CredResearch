package africa.credresearch.modules.document.infrastructure.persistence.repository;

import africa.credresearch.modules.document.infrastructure.persistence.entity.DocumentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {
    Optional<DocumentEntity> findByIdAndDeletedAtIsNull(UUID id);
    List<DocumentEntity> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID projectId);
}
