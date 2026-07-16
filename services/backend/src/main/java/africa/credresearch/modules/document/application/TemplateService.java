package africa.credresearch.modules.document.application;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.document.domain.model.FormatRule;
import africa.credresearch.modules.document.domain.model.Template;
import africa.credresearch.modules.document.domain.model.TemplateDetail;
import africa.credresearch.modules.document.domain.model.TemplateSection;
import africa.credresearch.modules.document.domain.port.TemplateRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Template catalogue + cloning (FR-TMPL-1/2/3). Templates are global or tenant-owned. */
@Service
public class TemplateService {

    private final TemplateRepository templates;

    public TemplateService(TemplateRepository templates) {
        this.templates = templates;
    }

    /** Global templates plus the caller's tenant templates. */
    public List<Template> list() {
        TenantContext ctx = TenantContextHolder.require();
        return templates.findVisible(ctx.institutionId());
    }

    /** A template with its ordered sections + format rule. Visible = global or same tenant. */
    public TemplateDetail get(UUID id) {
        Template t = requireVisible(id);
        return new TemplateDetail(t, templates.findSections(id),
                templates.findFormatRule(id).orElse(null));
    }

    /**
     * Clones an existing (visible) template into the caller's tenant, copying its sections and
     * format rule (FR-TMPL-2). Controller restricts this to DEPARTMENT_ADMIN+.
     */
    @Transactional
    public Template clone(UUID sourceId, String name, String level) {
        TenantContext ctx = TenantContextHolder.require();
        Template source = requireVisible(sourceId);
        Template created = templates.create(
                new Template(null, ctx.institutionId(), null,
                        name == null || name.isBlank() ? source.name() + " (copy)" : name,
                        level == null ? source.level() : level,
                        false, source.citationStyle()),
                ctx.userId());
        List<TemplateSection> copied = templates.findSections(sourceId).stream()
                .map(s -> new TemplateSection(null, created.id(), s.orderIndex(), s.chapter(),
                        s.heading(), s.guidance()))
                .toList();
        templates.createSections(copied);
        templates.findFormatRule(sourceId).ifPresent(r -> templates.createFormatRule(
                new FormatRule(null, created.id(), r.fontFamily(), r.fontSizePt(), r.lineSpacing(),
                        r.marginsJson(), r.headingNumbering(), r.citationStyle())));
        return created;
    }

    private Template requireVisible(UUID id) {
        TenantContext ctx = TenantContextHolder.require();
        Template t = templates.findById(id)
                .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "Template not found"));
        boolean visible = t.global()
                || (t.institutionId() != null && t.institutionId().equals(ctx.institutionId()))
                || ctx.isPlatformAdmin();
        if (!visible) {
            throw ApiException.forbidden("TEMPLATE_FORBIDDEN", "Template belongs to another tenant");
        }
        return t;
    }
}
