package africa.credresearch.modules.questionnaire.infrastructure.persistence.repository;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.entity.QuestionnaireEntity;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionnaireJpaRepository extends JpaRepository<QuestionnaireEntity, UUID> {
    List<QuestionnaireEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
