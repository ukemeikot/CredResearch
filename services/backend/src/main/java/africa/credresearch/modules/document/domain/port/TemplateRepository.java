package africa.credresearch.modules.document.domain.port;

import africa.credresearch.modules.document.domain.model.FormatRule;
import africa.credresearch.modules.document.domain.model.Template;
import africa.credresearch.modules.document.domain.model.TemplateSection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for template persistence. */
public interface TemplateRepository {

    /** Global templates plus those owned by {@code institutionId}. */
    List<Template> findVisible(UUID institutionId);

    Optional<Template> findById(UUID id);

    List<TemplateSection> findSections(UUID templateId);

    Optional<FormatRule> findFormatRule(UUID templateId);

    Template create(Template template, UUID createdBy);

    void createSections(List<TemplateSection> sections);

    FormatRule createFormatRule(FormatRule rule);
}
