package africa.credresearch.modules.document.infrastructure.persistence.repository;

import africa.credresearch.modules.document.infrastructure.persistence.entity.DocumentSectionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentSectionJpaRepository extends JpaRepository<DocumentSectionEntity, UUID> {

    List<DocumentSectionEntity> findByDocumentIdOrderByOrderIndexAsc(UUID documentId);

    /**
     * Atomic compare-and-set autosave: only updates when the stored version still equals the
     * expected one, so concurrent edits can't silently clobber each other (FR-DOC-3).
     * Native query with an explicit ::jsonb cast for the ProseMirror content.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update document_sections set content = cast(:content as jsonb), "
            + "content_text = :text, version = version + 1, updated_by = :by, updated_at = now() "
            + "where id = :id and version = :expected", nativeQuery = true)
    int autosave(@Param("id") UUID id, @Param("expected") int expectedVersion,
                 @Param("content") String content, @Param("text") String contentText,
                 @Param("by") UUID updatedBy);
}
