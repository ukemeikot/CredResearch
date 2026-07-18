package africa.credresearch.modules.questionnaire.interfaces.rest;

import africa.credresearch.modules.questionnaire.application.QuestionnaireService;
import africa.credresearch.modules.questionnaire.application.QuestionnaireService.QuestionnaireDetail;
import africa.credresearch.modules.questionnaire.application.QuestionnaireService.ResponseRow;
import africa.credresearch.modules.questionnaire.domain.model.Question;
import africa.credresearch.modules.questionnaire.domain.model.Questionnaire;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questionnaires")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Questionnaires", description = "Build surveys, publish tokenized public links, collect + export responses (Phase 7, FR-Q).")
public class QuestionnaireController {

    private final QuestionnaireService service;
    private final ObjectMapper mapper;

    public QuestionnaireController(QuestionnaireService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    public record CreateRequest(@NotNull UUID projectId, String title, String consentText) {}
    public record QuestionDto(String type, String prompt, JsonNode options, boolean required) {}
    public record UpdateRequest(String title, String consentText, List<QuestionDto> questions) {}
    public record PublishRequest(Integer expiresDays) {}
    public record PublishResponse(String token) {}

    public record QuestionView(UUID id, int orderIndex, String type, String prompt, JsonNode options, boolean required) {}
    public record QuestionnaireView(Questionnaire questionnaire, List<QuestionView> questions) {}

    @PostMapping
    @Operation(summary = "Create a questionnaire")
    public QuestionnaireView create(@RequestBody CreateRequest req) {
        return view(service.create(req.projectId(), req.title(), req.consentText()));
    }

    @GetMapping
    @Operation(summary = "List a project's questionnaires")
    public List<Questionnaire> list(@RequestParam UUID projectId) {
        return service.listByProject(projectId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a questionnaire with its questions")
    public QuestionnaireView get(@PathVariable UUID id) {
        return view(service.get(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update title/consent and replace questions")
    public QuestionnaireView update(@PathVariable UUID id, @RequestBody UpdateRequest req) {
        List<Question> qs = req.questions() == null ? null : req.questions().stream()
                .map(d -> new Question(null, id, 0, d.type(), d.prompt(),
                        d.options() == null || d.options().isNull() ? null : d.options().toString(), d.required()))
                .toList();
        return view(service.update(id, req.title(), req.consentText(), qs));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish + mint a public survey link (token returned once)")
    public PublishResponse publish(@PathVariable UUID id, @RequestBody(required = false) PublishRequest req) {
        return new PublishResponse(service.publish(id, req == null ? null : req.expiresDays()));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a questionnaire (stops accepting responses)")
    public void close(@PathVariable UUID id) {
        service.close(id);
    }

    @GetMapping("/{id}/responses")
    @Operation(summary = "List responses with answers")
    public List<ResponseRow> responses(@PathVariable UUID id) {
        return service.responses(id);
    }

    @GetMapping(value = "/{id}/responses.csv", produces = "text/csv")
    @Operation(summary = "Export responses as CSV (FR-Q)")
    public org.springframework.http.ResponseEntity<byte[]> csv(@PathVariable UUID id) {
        QuestionnaireView v = view(service.get(id));
        List<ResponseRow> rows = service.responses(id);
        StringBuilder sb = new StringBuilder();
        sb.append("submitted_at,consent");
        for (QuestionView q : v.questions()) sb.append(',').append(csv(q.prompt()));
        sb.append('\n');
        for (ResponseRow r : rows) {
            sb.append(r.response().submittedAt()).append(',').append(r.response().consentGiven());
            for (QuestionView q : v.questions()) {
                String val = r.answers().stream()
                        .filter(a -> a.questionId().equals(q.id()))
                        .map(a -> plain(a.valueJson()))
                        .findFirst().orElse("");
                sb.append(',').append(csv(val));
            }
            sb.append('\n');
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename("responses.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ── mapping / csv helpers ──────────────────────────────────────────────────
    private QuestionnaireView view(QuestionnaireDetail d) {
        List<QuestionView> qs = d.questions().stream()
                .map(q -> new QuestionView(q.id(), q.orderIndex(), q.type(), q.prompt(), parse(q.optionsJson()), q.required()))
                .toList();
        return new QuestionnaireView(d.questionnaire(), qs);
    }

    private JsonNode parse(String json) {
        if (json == null) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    /** Render a stored JSON answer value as plain text for CSV. */
    private String plain(String json) {
        if (json == null) return "";
        try {
            JsonNode n = mapper.readTree(json);
            if (n.isArray()) {
                StringBuilder b = new StringBuilder();
                for (JsonNode e : n) { if (b.length() > 0) b.append("; "); b.append(e.asText()); }
                return b.toString();
            }
            return n.asText();
        } catch (Exception e) {
            return json;
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        return (v.contains(",") || v.contains("\"") || v.contains("\n")) ? "\"" + v + "\"" : v;
    }
}
