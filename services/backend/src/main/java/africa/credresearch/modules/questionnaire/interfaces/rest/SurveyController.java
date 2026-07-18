package africa.credresearch.modules.questionnaire.interfaces.rest;

import africa.credresearch.modules.questionnaire.application.QuestionnaireService;
import africa.credresearch.modules.questionnaire.application.QuestionnaireService.Answer;
import africa.credresearch.modules.questionnaire.application.QuestionnaireService.PublicSurvey;
import africa.credresearch.modules.questionnaire.domain.model.Question;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * Public survey surface (Phase 7, FR-Q). No account: a respondent opens the tokenized link, sees
 * the consent + questions, and submits. Permitted without a bearer in SecurityConfig; the link
 * token scopes access to one questionnaire.
 */
@RestController
@RequestMapping("/api/v1/survey")
@Tag(name = "Survey (public)", description = "Account-less survey render + response submission via a link token.")
public class SurveyController {

    private final QuestionnaireService service;
    private final ObjectMapper mapper;

    public SurveyController(QuestionnaireService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    public record QuestionView(UUID id, String type, String prompt, JsonNode options, boolean required) {}
    public record SurveyView(String title, String consentText, List<QuestionView> questions) {}
    public record AnswerDto(UUID questionId, JsonNode value) {}
    public record SubmitRequest(boolean consentGiven, List<AnswerDto> answers) {}

    @GetMapping("/{token}")
    @Operation(summary = "Render the survey for a public link token")
    public SurveyView render(@PathVariable String token) {
        PublicSurvey s = service.render(token);
        return new SurveyView(s.title(), s.consentText(), s.questions().stream().map(this::view).toList());
    }

    @PostMapping("/{token}/responses")
    @Operation(summary = "Submit a response (consent + answers)")
    public void submit(@PathVariable String token, @RequestBody SubmitRequest req) {
        List<Answer> answers = req.answers() == null ? List.of() : req.answers().stream()
                .map(a -> new Answer(a.questionId(),
                        a.value() == null || a.value().isNull() ? null : a.value().toString()))
                .toList();
        service.submit(token, req.consentGiven(), answers, null);
    }

    private QuestionView view(Question q) {
        JsonNode opts = null;
        if (q.optionsJson() != null) {
            try {
                opts = mapper.readTree(q.optionsJson());
            } catch (Exception ignored) {
                // malformed options → render without them
            }
        }
        return new QuestionView(q.id(), q.type(), q.prompt(), opts, q.required());
    }
}
