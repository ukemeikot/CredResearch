package africa.credresearch.modules.identity.interfaces.rest;

import africa.credresearch.modules.identity.application.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Onboarding", description = "Institution onboarding: a user creates an institution and "
        + "becomes its INSTITUTION_ADMIN. Refresh tokens afterward to pick up the new tenant + role.")
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    public record OnboardInstitutionRequest(@NotBlank String name, String country, String type) {}

    @PostMapping("/institution")
    @Operation(summary = "Create an institution and become its admin",
            description = "Moves the caller out of their personal workspace. The client should call "
                    + "/auth/refresh afterward so the new access token reflects the new tenant + role.")
    public Map<String, UUID> createInstitution(@Valid @RequestBody OnboardInstitutionRequest req) {
        return Map.of("institutionId", service.createInstitution(req.name(), req.country(), req.type()));
    }
}
