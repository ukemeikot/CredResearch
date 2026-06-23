package africa.credresearch.modules.org.infrastructure.persistence.repository;

import africa.credresearch.modules.org.infrastructure.persistence.entity.InstitutionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionJpaRepository extends JpaRepository<InstitutionEntity, UUID> {
}
