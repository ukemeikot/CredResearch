package africa.credresearch.modules.org.infrastructure.persistence;

import africa.credresearch.modules.org.domain.model.Department;
import africa.credresearch.modules.org.domain.port.DepartmentRepository;
import africa.credresearch.modules.org.infrastructure.persistence.entity.DepartmentEntity;
import africa.credresearch.modules.org.infrastructure.persistence.repository.DepartmentJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DepartmentRepositoryAdapter implements DepartmentRepository {

    private final DepartmentJpaRepository jpa;

    public DepartmentRepositoryAdapter(DepartmentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Department create(Department department) {
        DepartmentEntity e = new DepartmentEntity();
        e.setInstitutionId(department.institutionId());
        e.setName(department.name());
        e.setCode(department.code());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Department> findByIdAndInstitution(UUID id, UUID institutionId) {
        return jpa.findByIdAndInstitutionId(id, institutionId).map(DepartmentRepositoryAdapter::toDomain);
    }

    @Override
    public List<Department> findByInstitution(UUID institutionId) {
        return jpa.findByInstitutionId(institutionId).stream()
                .map(DepartmentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByInstitutionAndName(UUID institutionId, String name) {
        return jpa.existsByInstitutionIdAndName(institutionId, name);
    }

    @Override
    @Transactional
    public void update(UUID id, UUID institutionId, String name, String code) {
        DepartmentEntity e = jpa.findByIdAndInstitutionId(id, institutionId).orElseThrow();
        if (name != null) e.setName(name);
        if (code != null) e.setCode(code);
        jpa.save(e);
    }

    static Department toDomain(DepartmentEntity e) {
        return new Department(e.getId(), e.getInstitutionId(), e.getName(), e.getCode());
    }
}
