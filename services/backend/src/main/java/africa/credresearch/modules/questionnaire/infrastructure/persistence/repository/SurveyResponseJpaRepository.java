package africa.credresearch.modules.questionnaire.infrastructure.persistence.repository;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.entity.SurveyResponseEntity;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SurveyResponseJpaRepository extends JpaRepository<SurveyResponseEntity, UUID> {
    List<SurveyResponseEntity> findBySurveyLinkIdInOrderBySubmittedAtAsc(List<UUID> surveyLinkIds);
    long countBySurveyLinkIdIn(List<UUID> surveyLinkIds);
}
