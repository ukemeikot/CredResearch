package africa.credresearch.modules.org.interfaces.rest;

import africa.credresearch.modules.org.application.DepartmentService;
import africa.credresearch.modules.org.domain.model.Department;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "Department management within the caller's institution. "
        + "All operations are tenant-scoped.")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    public record DepartmentResponse(UUID id, UUID institutionId, String name, String code) {
        static DepartmentResponse from(Department d) {
            return new DepartmentResponse(d.id(), d.institutionId(), d.name(), d.code());
        }
    }

    public record CreateDepartmentRequest(@NotBlank String name, String code) {}

    public record UpdateDepartmentRequest(String name, String code) {}

    @PostMapping
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    @Operation(summary = "Create a department", description = "Role: INSTITUTION_ADMIN. Name unique per institution.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created department"),
            @ApiResponse(responseCode = "409", description = "Name already exists in this institution", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public DepartmentResponse create(@Valid @RequestBody CreateDepartmentRequest req) {
        return DepartmentResponse.from(service.create(req.name(), req.code()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','INSTITUTION_ADMIN','PLATFORM_ADMIN')")
    @Operation(summary = "List departments", description = "Returns departments in the caller's institution.")
    @ApiResponse(responseCode = "200", description = "Departments")
    public List<DepartmentResponse> list() {
        return service.list().stream().map(DepartmentResponse::from).toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','INSTITUTION_ADMIN')")
    @Operation(summary = "Update a department", description = "Roles: DEPARTMENT_ADMIN or INSTITUTION_ADMIN (own tenant).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated department"),
            @ApiResponse(responseCode = "404", description = "Not found in this tenant", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public DepartmentResponse update(@PathVariable UUID id, @RequestBody UpdateDepartmentRequest req) {
        return DepartmentResponse.from(service.update(id, req.name(), req.code()));
    }
}
