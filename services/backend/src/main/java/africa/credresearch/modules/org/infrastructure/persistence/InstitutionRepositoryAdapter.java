package africa.credresearch.modules.org.infrastructure.persistence;

import africa.credresearch.modules.org.domain.model.Institution;
import africa.credresearch.modules.org.domain.port.InstitutionRepository;
import africa.credresearch.modules.org.infrastructure.persistence.entity.InstitutionEntity;
import africa.credresearch.modules.org.infrastructure.persistence.repository.InstitutionJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InstitutionRepositoryAdapter implements InstitutionRepository {

    private final InstitutionJpaRepository jpa;

    public InstitutionRepositoryAdapter(InstitutionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Institution create(Institution institution) {
        InstitutionEntity e = new InstitutionEntity();
        e.setName(institution.name());
        e.setCountry(institution.country());
        e.setType(institution.type());
        e.setPersonalTenant(institution.personalTenant());
        e.setStatus(institution.status() == null ? "active" : institution.status());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Institution> findById(UUID id) {
        return jpa.findById(id).map(InstitutionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void update(UUID id, String name, String country, String type) {
        InstitutionEntity e = jpa.findById(id).orElseThrow();
        if (name != null) e.setName(name);
        if (country != null) e.setCountry(country);
        if (type != null) e.setType(type);
        jpa.save(e);
    }

    static Institution toDomain(InstitutionEntity e) {
        return new Institution(e.getId(), e.getName(), e.getCountry(), e.getType(),
                e.isPersonalTenant(), e.getStatus());
    }
}
