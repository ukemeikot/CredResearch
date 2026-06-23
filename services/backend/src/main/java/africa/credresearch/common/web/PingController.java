package africa.credresearch.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint under the versioned API base path ({@code /api/v1}).
 * Phase-0 smoke check; real endpoints live in each feature module's {@code interfaces.rest}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "Liveness / smoke endpoints.")
public class PingController {

    @GetMapping("/ping")
    @SecurityRequirements
    @Operation(summary = "Ping", description = "Public liveness check.")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "backend");
    }
}
