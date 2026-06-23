package africa.credresearch.modules.org.domain.port;

import africa.credresearch.modules.org.domain.model.Institution;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository {

    Institution create(Institution institution);

    Optional<Institution> findById(UUID id);

    void update(UUID id, String name, String country, String type);
}
