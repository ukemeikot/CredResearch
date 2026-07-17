package africa.credresearch.modules.disclosure.infrastructure.persistence.repository;

import africa.credresearch.modules.disclosure.infrastructure.persistence.entity.DisclosureEntryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisclosureEntryJpaRepository extends JpaRepository<DisclosureEntryEntity, UUID> {
    List<DisclosureEntryEntity> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
    Optional<DisclosureEntryEntity> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
