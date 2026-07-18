package africa.credresearch.modules.questionnaire.infrastructure.persistence;

import africa.credresearch.modules.questionnaire.domain.model.Question;
import africa.credresearch.modules.questionnaire.domain.model.Questionnaire;
import africa.credresearch.modules.questionnaire.domain.model.SurveyAnswer;
import africa.credresearch.modules.questionnaire.domain.model.SurveyLink;
import africa.credresearch.modules.questionnaire.domain.model.SurveyResponse;
import africa.credresearch.modules.questionnaire.domain.port.QuestionnaireRepository;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.entity.*;
import africa.credresearch.modules.questionnaire.infrastructure.persistence.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionnaireRepositoryAdapter implements QuestionnaireRepository {

    private final QuestionnaireJpaRepository questionnaires;
    private final QuestionJpaRepository questions;
    private final SurveyLinkJpaRepository links;
    private final SurveyResponseJpaRepository responses;
    private final SurveyAnswerJpaRepository answers;

    public QuestionnaireRepositoryAdapter(QuestionnaireJpaRepository questionnaires, QuestionJpaRepository questions,
                                          SurveyLinkJpaRepository links, SurveyResponseJpaRepository responses,
                                          SurveyAnswerJpaRepository answers) {
        this.questionnaires = questionnaires;
        this.questions = questions;
        this.links = links;
        this.responses = responses;
        this.answers = answers;
    }

    @Override
    public Questionnaire createQuestionnaire(Questionnaire q) {
        QuestionnaireEntity e = new QuestionnaireEntity();
        e.setProjectId(q.projectId());
        e.setTitle(q.title());
        e.setConsentText(q.consentText());
        e.setStatus(q.status() == null ? "DRAFT" : q.status());
        return toQ(questionnaires.save(e));
    }

    @Override
    public Optional<Questionnaire> findQuestionnaire(UUID id) {
        return questionnaires.findById(id).map(QuestionnaireRepositoryAdapter::toQ);
    }

    @Override
    public List<Questionnaire> findByProject(UUID projectId) {
        return questionnaires.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(QuestionnaireRepositoryAdapter::toQ).toList();
    }

    @Override
    public Questionnaire updateQuestionnaire(UUID id, String title, String consentText, String status) {
        QuestionnaireEntity e = questionnaires.findById(id).orElseThrow();
        if (title != null) e.setTitle(title);
        if (consentText != null) e.setConsentText(consentText);
        if (status != null) e.setStatus(status);
        return toQ(questionnaires.save(e));
    }

    @Override
    public void replaceQuestions(UUID questionnaireId, List<Question> qs) {
        questions.deleteByQuestionnaireId(questionnaireId);
        int i = 0;
        for (Question q : qs) {
            QuestionEntity e = new QuestionEntity();
            e.setQuestionnaireId(questionnaireId);
            e.setOrderIndex(i++);
            e.setType(q.type());
            e.setPrompt(q.prompt());
            e.setOptionsJson(q.optionsJson());
            e.setRequired(q.required());
            questions.save(e);
        }
    }

    @Override
    public List<Question> findQuestions(UUID questionnaireId) {
        return questions.findByQuestionnaireIdOrderByOrderIndexAsc(questionnaireId).stream().map(QuestionnaireRepositoryAdapter::toQuestion).toList();
    }

    @Override
    public SurveyLink createLink(UUID questionnaireId, String tokenHash, Instant expiresAt) {
        SurveyLinkEntity e = new SurveyLinkEntity();
        e.setQuestionnaireId(questionnaireId);
        e.setTokenHash(tokenHash);
        e.setActive(true);
        e.setExpiresAt(expiresAt);
        return toLink(links.save(e));
    }

    @Override
    public List<SurveyLink> findLinks(UUID questionnaireId) {
        return links.findByQuestionnaireId(questionnaireId).stream().map(QuestionnaireRepositoryAdapter::toLink).toList();
    }

    @Override
    public Optional<UUID[]> findActiveLink(String tokenHash, Instant now) {
        return links.findByTokenHash(tokenHash)
                .filter(SurveyLinkEntity::isActive)
                .filter(l -> l.getExpiresAt() == null || l.getExpiresAt().isAfter(now))
                .map(l -> new UUID[] { l.getId(), l.getQuestionnaireId() });
    }

    @Override
    public SurveyResponse createResponse(UUID surveyLinkId, boolean consentGiven, String respondentMeta) {
        SurveyResponseEntity e = new SurveyResponseEntity();
        e.setSurveyLinkId(surveyLinkId);
        e.setConsentGiven(consentGiven);
        e.setRespondentMeta(respondentMeta);
        return toResponse(responses.save(e));
    }

    @Override
    public void addAnswer(UUID responseId, UUID questionId, String valueJson) {
        SurveyAnswerEntity e = new SurveyAnswerEntity();
        e.setSurveyResponseId(responseId);
        e.setQuestionId(questionId);
        e.setValueJson(valueJson);
        answers.save(e);
    }

    @Override
    public List<SurveyResponse> findResponses(List<UUID> surveyLinkIds) {
        if (surveyLinkIds.isEmpty()) return List.of();
        return responses.findBySurveyLinkIdInOrderBySubmittedAtAsc(surveyLinkIds).stream().map(QuestionnaireRepositoryAdapter::toResponse).toList();
    }

    @Override
    public List<SurveyAnswer> findAnswers(List<UUID> responseIds) {
        if (responseIds.isEmpty()) return List.of();
        return answers.findBySurveyResponseIdIn(responseIds).stream().map(QuestionnaireRepositoryAdapter::toAnswer).toList();
    }

    @Override
    public long countResponses(List<UUID> surveyLinkIds) {
        return surveyLinkIds.isEmpty() ? 0 : responses.countBySurveyLinkIdIn(surveyLinkIds);
    }

    private static Questionnaire toQ(QuestionnaireEntity e) {
        return new Questionnaire(e.getId(), e.getProjectId(), e.getTitle(), e.getConsentText(), e.getStatus(), e.getCreatedAt());
    }
    private static Question toQuestion(QuestionEntity e) {
        return new Question(e.getId(), e.getQuestionnaireId(), e.getOrderIndex(), e.getType(), e.getPrompt(), e.getOptionsJson(), e.isRequired());
    }
    private static SurveyLink toLink(SurveyLinkEntity e) {
        return new SurveyLink(e.getId(), e.getQuestionnaireId(), e.isActive(), e.getExpiresAt(), e.getCreatedAt());
    }
    private static SurveyResponse toResponse(SurveyResponseEntity e) {
        return new SurveyResponse(e.getId(), e.getSurveyLinkId(), e.isConsentGiven(), e.getSubmittedAt());
    }
    private static SurveyAnswer toAnswer(SurveyAnswerEntity e) {
        return new SurveyAnswer(e.getId(), e.getSurveyResponseId(), e.getQuestionId(), e.getValueJson());
    }
}
