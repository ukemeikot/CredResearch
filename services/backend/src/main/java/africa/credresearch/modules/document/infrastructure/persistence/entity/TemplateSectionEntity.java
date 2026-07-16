package africa.credresearch.modules.document.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "template_sections")
public class TemplateSectionEntity {
    @Id private UUID id;
    @Column(name = "template_id", nullable = false) private UUID templateId;
    @Column(name = "order_index", nullable = false) private int orderIndex;
    private String chapter;
    @Column(nullable = false) private String heading;
    private String guidance;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); }

    public UUID getId() { return id; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID v) { templateId = v; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int v) { orderIndex = v; }
    public String getChapter() { return chapter; }
    public void setChapter(String v) { chapter = v; }
    public String getHeading() { return heading; }
    public void setHeading(String v) { heading = v; }
    public String getGuidance() { return guidance; }
    public void setGuidance(String v) { guidance = v; }
}
