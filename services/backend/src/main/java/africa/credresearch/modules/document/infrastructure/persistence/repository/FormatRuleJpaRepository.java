package africa.credresearch.modules.document.infrastructure.persistence.repository;

import africa.credresearch.modules.document.infrastructure.persistence.entity.FormatRuleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormatRuleJpaRepository extends JpaRepository<FormatRuleEntity, UUID> {
    List<FormatRuleEntity> findByTemplateId(UUID templateId);
}
