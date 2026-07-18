package africa.credresearch.modules.questionnaire.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.util.TokenHasher;
import africa.credresearch.modules.questionnaire.domain.model.Question;
import africa.credresearch.modules.questionnaire.domain.model.Questionnaire;
import africa.credresearch.modules.questionnaire.domain.model.SurveyAnswer;
import africa.credresearch.modules.questionnaire.domain.model.SurveyLink;
import africa.credresearch.modules.questionnaire.domain.model.SurveyResponse;
import africa.credresearch.modules.questionnaire.domain.port.QuestionnaireRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Questionnaire builder + public data collection (Phase 7, FR-Q): build questions, publish a
 * tokenized public link, collect consented responses at that link, and export responses as CSV.
 * Authoring is project-membership gated; response submission is public via the link token.
 */
@Service
public class QuestionnaireService {

    public record QuestionnaireDetail(Questionnaire questionnaire, List<Question> questions) {}

    /** Public survey render: enough to display + submit, no owner data. */
    public record PublicSurvey(UUID surveyLinkId, String title, String consentText, List<Question> questions) {}

    /** One response with its answers, for the responses view / CSV. */
    public record ResponseRow(SurveyResponse response, List<SurveyAnswer> answers) {}

    private final QuestionnaireRepository repo;
    private final ProjectAccessGuard projectAccess;

    public QuestionnaireService(QuestionnaireRepository repo, ProjectAccessGuard projectAccess) {
        this.repo = repo;
        this.projectAccess = projectAccess;
    }

    // ── Authoring (member-gated) ─────────────────────────────────────────────
    @Transactional
    public QuestionnaireDetail create(UUID projectId, String title, String consentText) {
        projectAccess.requireMember(projectId);
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("TITLE_REQUIRED", "A questionnaire title is required.");
        }
        Questionnaire q = repo.createQuestionnaire(new Questionnaire(null, projectId, title, consentText, "DRAFT", null));
        return new QuestionnaireDetail(q, List.of());
    }

    public List<Questionnaire> listByProject(UUID projectId) {
        projectAccess.requireMember(projectId);
        return repo.findByProject(projectId);
    }

    public QuestionnaireDetail get(UUID id) {
        Questionnaire q = requireMember(id);
        return new QuestionnaireDetail(q, repo.findQuestions(id));
    }

    @Transactional
    public QuestionnaireDetail update(UUID id, String title, String consentText, List<Question> questions) {
        requireMember(id);
        Questionnaire q = repo.updateQuestionnaire(id, title, consentText, null);
        if (questions != null) {
            repo.replaceQuestions(id, questions);
        }
        return new QuestionnaireDetail(q, repo.findQuestions(id));
    }

    /** Publish the questionnaire and mint a tokenized public link; returns the raw token once. */
    @Transactional
    public String publish(UUID id, Integer expiresDays) {
        requireMember(id);
        if (repo.findQuestions(id).isEmpty()) {
            throw ApiException.badRequest("NO_QUESTIONS", "Add at least one question before publishing.");
        }
        repo.updateQuestionnaire(id, null, null, "PUBLISHED");
        String raw = TokenHasher.randomToken();
        Instant expiry = expiresDays == null ? null : Instant.now().plusSeconds(expiresDays * 86400L);
        repo.createLink(id, TokenHasher.sha256(raw), expiry);
        return raw;
    }

    @Transactional
    public void close(UUID id) {
        requireMember(id);
        repo.updateQuestionnaire(id, null, null, "CLOSED");
    }

    // ── Public data collection (token-scoped, no account) ────────────────────
    public PublicSurvey render(String rawToken) {
        UUID[] link = requireLink(rawToken);
        Questionnaire q = repo.findQuestionnaire(link[1]).orElseThrow(() ->
                ApiException.badRequest("SURVEY_UNAVAILABLE", "This survey is unavailable."));
        if (!"PUBLISHED".equals(q.status())) {
            throw ApiException.badRequest("SURVEY_CLOSED", "This survey is not accepting responses.");
        }
        return new PublicSurvey(link[0], q.title(), q.consentText(), repo.findQuestions(q.id()));
    }

    @Transactional
    public void submit(String rawToken, boolean consentGiven, List<Answer> answers, String respondentMeta) {
        UUID[] link = requireLink(rawToken);
        Questionnaire q = repo.findQuestionnaire(link[1]).orElseThrow(() ->
                ApiException.badRequest("SURVEY_UNAVAILABLE", "This survey is unavailable."));
        if (!"PUBLISHED".equals(q.status())) {
            throw ApiException.badRequest("SURVEY_CLOSED", "This survey is not accepting responses.");
        }
        if (q.consentText() != null && !q.consentText().isBlank() && !consentGiven) {
            throw ApiException.badRequest("CONSENT_REQUIRED", "Consent is required to submit this survey.");
        }
        SurveyResponse r = repo.createResponse(link[0], consentGiven, respondentMeta);
        if (answers != null) {
            for (Answer a : answers) {
                if (a != null && a.questionId() != null) {
                    repo.addAnswer(r.id(), a.questionId(), a.valueJson());
                }
            }
        }
    }

    public record Answer(UUID questionId, String valueJson) {}

    // ── Responses / export (member-gated) ────────────────────────────────────
    public List<ResponseRow> responses(UUID id) {
        requireMember(id);
        List<UUID> linkIds = repo.findLinks(id).stream().map(SurveyLink::id).toList();
        List<SurveyResponse> resp = repo.findResponses(linkIds);
        List<UUID> respIds = resp.stream().map(SurveyResponse::id).toList();
        List<SurveyAnswer> all = repo.findAnswers(respIds);
        return resp.stream()
                .map(r -> new ResponseRow(r, all.stream().filter(a -> a.surveyResponseId().equals(r.id())).toList()))
                .toList();
    }

    public long responseCount(UUID id) {
        requireMember(id);
        return repo.countResponses(repo.findLinks(id).stream().map(SurveyLink::id).toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private Questionnaire requireMember(UUID questionnaireId) {
        Questionnaire q = repo.findQuestionnaire(questionnaireId)
                .orElseThrow(() -> ApiException.notFound("QUESTIONNAIRE_NOT_FOUND", "Questionnaire not found"));
        projectAccess.requireMember(q.projectId());
        return q;
    }

    private UUID[] requireLink(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.badRequest("INVALID_LINK", "Missing survey link.");
        }
        return repo.findActiveLink(TokenHasher.sha256(rawToken), Instant.now())
                .orElseThrow(() -> ApiException.badRequest("INVALID_LINK", "This survey link is invalid or has expired."));
    }
}
