package africa.credresearch.modules.admin.interfaces.rest;

import africa.credresearch.modules.admin.application.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin", description = "Platform-admin dashboards (Phase 10, FR-ADM). PLATFORM_ADMIN only.")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    @Operation(summary = "Headline platform statistics")
    public AdminService.Stats stats() {
        return service.stats();
    }

    @GetMapping("/users")
    @Operation(summary = "Recent users")
    public List<Map<String, Object>> users(@RequestParam(defaultValue = "50") int limit) {
        return service.recentUsers(limit);
    }

    @GetMapping("/institutions")
    @Operation(summary = "Institutions")
    public List<Map<String, Object>> institutions() {
        return service.institutions();
    }
}
