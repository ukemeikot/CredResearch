package africa.credresearch.modules.org.interfaces.rest;

import africa.credresearch.modules.org.application.InstitutionService;
import africa.credresearch.modules.org.domain.model.Institution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/institutions")
@Tag(name = "Institutions", description = "Institution (tenant) management. Tenant-scoped: "
        + "non-platform admins may only access their own institution.")
public class InstitutionController {

    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    public record InstitutionResponse(UUID id, String name, String country, String type,
                                      boolean personalTenant, String status) {
        static InstitutionResponse from(Institution i) {
            return new InstitutionResponse(i.id(), i.name(), i.country(), i.type(),
                    i.personalTenant(), i.status());
        }
    }

    public record CreateInstitutionRequest(@NotBlank String name, String country, String type) {}

    public record UpdateInstitutionRequest(String name, String country, String type) {}

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','INSTITUTION_ADMIN','PLATFORM_ADMIN')")
    @Operation(summary = "Get an institution",
            description = "Roles: DEPARTMENT_ADMIN+. Returns 403 for another tenant's institution.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institution"),
            @ApiResponse(responseCode = "403", description = "Cross-tenant access denied", content = @io.swagger.v3.oas.annotations.media.Content()),
            @ApiResponse(responseCode = "404", description = "Not found", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    public InstitutionResponse get(@PathVariable UUID id) {
        return InstitutionResponse.from(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create an institution", description = "Role: PLATFORM_ADMIN only.")
    @ApiResponse(responseCode = "200", description = "Created institution")
    public InstitutionResponse create(@Valid @RequestBody CreateInstitutionRequest req) {
        return InstitutionResponse.from(service.create(req.name(), req.country(), req.type()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','PLATFORM_ADMIN')")
    @Operation(summary = "Update an institution",
            description = "Roles: INSTITUTION_ADMIN (own tenant) or PLATFORM_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Updated institution")
    public InstitutionResponse update(@PathVariable UUID id, @RequestBody UpdateInstitutionRequest req) {
        return InstitutionResponse.from(service.update(id, req.name(), req.country(), req.type()));
    }
}
