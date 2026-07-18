package africa.credresearch.modules.questionnaire.infrastructure.persistence.repository;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.entity.SurveyAnswerEntity;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SurveyAnswerJpaRepository extends JpaRepository<SurveyAnswerEntity, UUID> {
    List<SurveyAnswerEntity> findBySurveyResponseIdIn(List<UUID> responseIds);
}
