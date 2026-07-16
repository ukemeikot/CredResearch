package africa.credresearch.modules.document.infrastructure.persistence.repository;

import africa.credresearch.modules.document.infrastructure.persistence.entity.TemplateEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateJpaRepository extends JpaRepository<TemplateEntity, UUID> {

    @Query("select t from TemplateEntity t where t.deletedAt is null "
            + "and (t.global = true or t.institutionId = :inst) order by t.level, t.name")
    List<TemplateEntity> findVisible(@Param("inst") UUID institutionId);

    Optional<TemplateEntity> findByIdAndDeletedAtIsNull(UUID id);
}
