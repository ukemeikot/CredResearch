package africa.credresearch.modules.document.interfaces.rest;

import africa.credresearch.modules.document.application.TemplateService;
import africa.credresearch.modules.document.domain.model.FormatRule;
import africa.credresearch.modules.document.domain.model.Template;
import africa.credresearch.modules.document.domain.model.TemplateDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Templates", description = "Document templates (global + tenant). Sections + format rules. "
        + "FR-TMPL-1/2/3.")
public class TemplateController {

    private final TemplateService service;
    private final ObjectMapper objectMapper;

    public TemplateController(TemplateService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    public record TemplateResponse(UUID id, String name, String level, boolean global, String citationStyle) {
        static TemplateResponse from(Template t) {
            return new TemplateResponse(t.id(), t.name(), t.level(), t.global(), t.citationStyle());
        }
    }

    public record SectionResponse(UUID id, int orderIndex, String chapter, String heading, String guidance) {}

    public record FormatRuleResponse(String fontFamily, BigDecimal fontSizePt, BigDecimal lineSpacing,
                                     JsonNode margins, String headingNumbering, String citationStyle) {}

    public record TemplateDetailResponse(TemplateResponse template, List<SectionResponse> sections,
                                         FormatRuleResponse formatRule) {}

    public record CloneTemplateRequest(@NotNull UUID sourceTemplateId, String name, String level) {}

    @GetMapping
    @Operation(summary = "List templates", description = "Global templates plus the caller's tenant templates.")
    public List<TemplateResponse> list() {
        return service.list().stream().map(TemplateResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a template with sections + format rule")
    public TemplateDetailResponse get(@PathVariable UUID id) {
        return toDetail(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','INSTITUTION_ADMIN','PLATFORM_ADMIN')")
    @Operation(summary = "Clone a template into your tenant (FR-TMPL-2)")
    public TemplateResponse clone(@Valid @RequestBody CloneTemplateRequest req) {
        return TemplateResponse.from(service.clone(req.sourceTemplateId(), req.name(), req.level()));
    }

    private TemplateDetailResponse toDetail(TemplateDetail d) {
        List<SectionResponse> sections = d.sections().stream()
                .map(s -> new SectionResponse(s.id(), s.orderIndex(), s.chapter(), s.heading(), s.guidance()))
                .toList();
        FormatRule r = d.formatRule();
        FormatRuleResponse rule = r == null ? null : new FormatRuleResponse(
                r.fontFamily(), r.fontSizePt(), r.lineSpacing(), parse(r.marginsJson()),
                r.headingNumbering(), r.citationStyle());
        return new TemplateDetailResponse(TemplateResponse.from(d.template()), sections, rule);
    }

    private JsonNode parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
