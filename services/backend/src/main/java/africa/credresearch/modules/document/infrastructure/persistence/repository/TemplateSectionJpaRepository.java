package africa.credresearch.modules.document.infrastructure.persistence.repository;

import africa.credresearch.modules.document.infrastructure.persistence.entity.TemplateSectionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateSectionJpaRepository extends JpaRepository<TemplateSectionEntity, UUID> {
    List<TemplateSectionEntity> findByTemplateIdOrderByOrderIndexAsc(UUID templateId);
}
