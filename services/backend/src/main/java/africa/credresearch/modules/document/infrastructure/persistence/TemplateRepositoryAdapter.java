package africa.credresearch.modules.document.infrastructure.persistence;

import africa.credresearch.modules.document.domain.model.FormatRule;
import africa.credresearch.modules.document.domain.model.Template;
import africa.credresearch.modules.document.domain.model.TemplateSection;
import africa.credresearch.modules.document.domain.port.TemplateRepository;
import africa.credresearch.modules.document.infrastructure.persistence.entity.FormatRuleEntity;
import africa.credresearch.modules.document.infrastructure.persistence.entity.TemplateEntity;
import africa.credresearch.modules.document.infrastructure.persistence.entity.TemplateSectionEntity;
import africa.credresearch.modules.document.infrastructure.persistence.repository.FormatRuleJpaRepository;
import africa.credresearch.modules.document.infrastructure.persistence.repository.TemplateJpaRepository;
import africa.credresearch.modules.document.infrastructure.persistence.repository.TemplateSectionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemplateRepositoryAdapter implements TemplateRepository {

    private final TemplateJpaRepository templates;
    private final TemplateSectionJpaRepository sections;
    private final FormatRuleJpaRepository rules;

    public TemplateRepositoryAdapter(TemplateJpaRepository templates,
                                     TemplateSectionJpaRepository sections,
                                     FormatRuleJpaRepository rules) {
        this.templates = templates;
        this.sections = sections;
        this.rules = rules;
    }

    @Override
    public List<Template> findVisible(UUID institutionId) {
        return templates.findVisible(institutionId).stream().map(TemplateRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Template> findById(UUID id) {
        return templates.findByIdAndDeletedAtIsNull(id).map(TemplateRepositoryAdapter::toDomain);
    }

    @Override
    public List<TemplateSection> findSections(UUID templateId) {
        return sections.findByTemplateIdOrderByOrderIndexAsc(templateId).stream()
                .map(e -> new TemplateSection(e.getId(), e.getTemplateId(), e.getOrderIndex(),
                        e.getChapter(), e.getHeading(), e.getGuidance()))
                .toList();
    }

    @Override
    public Optional<FormatRule> findFormatRule(UUID templateId) {
        return rules.findByTemplateId(templateId).stream().findFirst().map(TemplateRepositoryAdapter::toRule);
    }

    @Override
    public Template create(Template t, UUID createdBy) {
        TemplateEntity e = new TemplateEntity();
        e.setInstitutionId(t.institutionId());
        e.setDepartmentId(t.departmentId());
        e.setName(t.name());
        e.setLevel(t.level());
        e.setGlobal(t.global());
        e.setCitationStyle(t.citationStyle() == null ? "APA" : t.citationStyle());
        e.setCreatedBy(createdBy);
        e.setUpdatedBy(createdBy);
        return toDomain(templates.save(e));
    }

    @Override
    public void createSections(List<TemplateSection> list) {
        List<TemplateSectionEntity> entities = list.stream().map(s -> {
            TemplateSectionEntity e = new TemplateSectionEntity();
            e.setTemplateId(s.templateId());
            e.setOrderIndex(s.orderIndex());
            e.setChapter(s.chapter());
            e.setHeading(s.heading());
            e.setGuidance(s.guidance());
            return e;
        }).toList();
        sections.saveAll(entities);
    }

    @Override
    public FormatRule createFormatRule(FormatRule r) {
        FormatRuleEntity e = new FormatRuleEntity();
        e.setTemplateId(r.templateId());
        e.setFontFamily(r.fontFamily());
        e.setFontSizePt(r.fontSizePt());
        e.setLineSpacing(r.lineSpacing());
        e.setMarginsJson(r.marginsJson());
        e.setHeadingNumbering(r.headingNumbering());
        e.setCitationStyle(r.citationStyle());
        return toRule(rules.save(e));
    }

    static Template toDomain(TemplateEntity e) {
        return new Template(e.getId(), e.getInstitutionId(), e.getDepartmentId(), e.getName(),
                e.getLevel(), e.isGlobal(), e.getCitationStyle());
    }

    static FormatRule toRule(FormatRuleEntity e) {
        return new FormatRule(e.getId(), e.getTemplateId(), e.getFontFamily(), e.getFontSizePt(),
                e.getLineSpacing(), e.getMarginsJson(), e.getHeadingNumbering(), e.getCitationStyle());
    }
}
