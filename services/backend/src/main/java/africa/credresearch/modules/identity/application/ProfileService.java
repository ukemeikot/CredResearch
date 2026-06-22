package africa.credresearch.modules.identity.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    public User currentUser() {
        UUID userId = TenantContextHolder.require().userId();
        return users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

    public User updateCurrent(String fullName, String academicLevel, String fieldOfStudy, String orcid) {
        User current = currentUser();
        users.updateProfile(current.id(),
                fullName != null ? fullName : current.fullName(),
                academicLevel != null ? academicLevel : current.academicLevel(),
                fieldOfStudy != null ? fieldOfStudy : current.fieldOfStudy(),
                orcid != null ? orcid : current.orcid());
        return currentUser();
    }
}
