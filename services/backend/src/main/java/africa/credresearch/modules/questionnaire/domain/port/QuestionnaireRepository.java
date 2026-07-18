package africa.credresearch.modules.questionnaire.domain.port;

import africa.credresearch.modules.questionnaire.domain.model.Question;
import africa.credresearch.modules.questionnaire.domain.model.Questionnaire;
import africa.credresearch.modules.questionnaire.domain.model.SurveyAnswer;
import africa.credresearch.modules.questionnaire.domain.model.SurveyLink;
import africa.credresearch.modules.questionnaire.domain.model.SurveyResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository {

    Questionnaire createQuestionnaire(Questionnaire q);
    Optional<Questionnaire> findQuestionnaire(UUID id);
    List<Questionnaire> findByProject(UUID projectId);
    Questionnaire updateQuestionnaire(UUID id, String title, String consentText, String status);

    /** Replace all questions of a questionnaire (builder saves the whole set). */
    void replaceQuestions(UUID questionnaireId, List<Question> questions);
    List<Question> findQuestions(UUID questionnaireId);

    SurveyLink createLink(UUID questionnaireId, String tokenHash, Instant expiresAt);
    List<SurveyLink> findLinks(UUID questionnaireId);
    /** Resolve an active, unexpired link by token hash; returns the (linkId, questionnaireId). */
    Optional<UUID[]> findActiveLink(String tokenHash, Instant now); // [linkId, questionnaireId]

    SurveyResponse createResponse(UUID surveyLinkId, boolean consentGiven, String respondentMeta);
    void addAnswer(UUID responseId, UUID questionId, String valueJson);
    List<SurveyResponse> findResponses(List<UUID> surveyLinkIds);
    List<SurveyAnswer> findAnswers(List<UUID> responseIds);
    long countResponses(List<UUID> surveyLinkIds);
}
