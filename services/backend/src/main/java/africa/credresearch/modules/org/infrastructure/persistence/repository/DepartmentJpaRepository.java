package africa.credresearch.modules.org.infrastructure.persistence.repository;

import africa.credresearch.modules.org.infrastructure.persistence.entity.DepartmentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID> {

    Optional<DepartmentEntity> findByIdAndInstitutionId(UUID id, UUID institutionId);

    List<DepartmentEntity> findByInstitutionId(UUID institutionId);

    boolean existsByInstitutionIdAndName(UUID institutionId, String name);
}
