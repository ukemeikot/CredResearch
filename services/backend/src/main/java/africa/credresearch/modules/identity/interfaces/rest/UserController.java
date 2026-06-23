package africa.credresearch.modules.identity.interfaces.rest;

import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.identity.application.ProfileService;
import africa.credresearch.modules.identity.domain.model.User;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    public record ProfileResponse(UUID id, String email, String fullName, UUID institutionId,
                                  UUID departmentId, String academicLevel, String fieldOfStudy,
                                  String orcid, boolean emailVerified, Set<String> roles) {
        static ProfileResponse from(User u, Set<String> roles) {
            return new ProfileResponse(u.id(), u.email(), u.fullName(), u.institutionId(),
                    u.departmentId(), u.academicLevel(), u.fieldOfStudy(), u.orcid(),
                    u.isEmailVerified(), roles);
        }
    }

    public record UpdateProfileRequest(String fullName, String academicLevel,
                                       String fieldOfStudy, String orcid) {}

    @GetMapping("/me")
    public ProfileResponse me() {
        Set<String> roles = TenantContextHolder.require().roles();
        return ProfileResponse.from(profileService.currentUser(), roles);
    }

    @PatchMapping("/me")
    public ProfileResponse updateMe(@RequestBody UpdateProfileRequest req) {
        Set<String> roles = TenantContextHolder.require().roles();
        User updated = profileService.updateCurrent(
                req.fullName(), req.academicLevel(), req.fieldOfStudy(), req.orcid());
        return ProfileResponse.from(updated, roles);
    }
}
