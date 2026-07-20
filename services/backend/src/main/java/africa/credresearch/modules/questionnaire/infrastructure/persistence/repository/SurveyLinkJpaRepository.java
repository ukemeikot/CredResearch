package africa.credresearch.modules.questionnaire.infrastructure.persistence.repository;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.entity.SurveyLinkEntity;
import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SurveyLinkJpaRepository extends JpaRepository<SurveyLinkEntity, UUID> {
    Optional<SurveyLinkEntity> findByTokenHash(String tokenHash);
    List<SurveyLinkEntity> findByQuestionnaireId(UUID questionnaireId);
}
