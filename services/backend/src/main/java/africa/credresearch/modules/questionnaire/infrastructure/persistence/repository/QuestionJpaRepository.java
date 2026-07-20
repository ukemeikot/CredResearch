package africa.credresearch.modules.questionnaire.infrastructure.persistence.repository;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.entity.QuestionEntity;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, UUID> {
    List<QuestionEntity> findByQuestionnaireIdOrderByOrderIndexAsc(UUID questionnaireId);
    void deleteByQuestionnaireId(UUID questionnaireId);
}
