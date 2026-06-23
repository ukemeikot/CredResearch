package africa.credresearch.modules.org.domain.port;

import africa.credresearch.modules.org.domain.model.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {

    Department create(Department department);

    Optional<Department> findByIdAndInstitution(UUID id, UUID institutionId);

    List<Department> findByInstitution(UUID institutionId);

    boolean existsByInstitutionAndName(UUID institutionId, String name);

    void update(UUID id, UUID institutionId, String name, String code);
}
