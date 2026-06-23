package africa.credresearch.modules.org.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.org.domain.model.Department;
import africa.credresearch.modules.org.domain.port.DepartmentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {

    private final DepartmentRepository departments;

    public DepartmentService(DepartmentRepository departments) {
        this.departments = departments;
    }

    public Department create(String name, String code) {
        UUID institutionId = TenantContextHolder.require().institutionId();
        if (departments.existsByInstitutionAndName(institutionId, name)) {
            throw ApiException.conflict("DEPARTMENT_EXISTS", "A department with that name already exists");
        }
        return departments.create(new Department(null, institutionId, name, code));
    }

    public List<Department> list() {
        return departments.findByInstitution(TenantContextHolder.require().institutionId());
    }

    public Department update(UUID id, String name, String code) {
        TenantContext ctx = TenantContextHolder.require();
        departments.findByIdAndInstitution(id, ctx.institutionId())
                .orElseThrow(() -> ApiException.notFound("DEPARTMENT_NOT_FOUND", "Department not found"));
        departments.update(id, ctx.institutionId(), name, code);
        return departments.findByIdAndInstitution(id, ctx.institutionId()).orElseThrow();
    }
}
