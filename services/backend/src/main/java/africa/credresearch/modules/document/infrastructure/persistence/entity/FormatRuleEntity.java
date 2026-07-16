package africa.credresearch.modules.document.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_format_rules")
public class FormatRuleEntity {
    @Id private UUID id;
    @Column(name = "template_id", nullable = false) private UUID templateId;
    @Column(name = "font_family", nullable = false) private String fontFamily;
    @Column(name = "font_size_pt", nullable = false) private BigDecimal fontSizePt;
    @Column(name = "line_spacing", nullable = false) private BigDecimal lineSpacing;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "margins_json", columnDefinition = "jsonb") private String marginsJson;
    @Column(name = "heading_numbering", nullable = false) private String headingNumbering;
    @Column(name = "citation_style", nullable = false) private String citationStyle;

    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); }

    public UUID getId() { return id; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID v) { templateId = v; }
    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String v) { fontFamily = v; }
    public BigDecimal getFontSizePt() { return fontSizePt; }
    public void setFontSizePt(BigDecimal v) { fontSizePt = v; }
    public BigDecimal getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(BigDecimal v) { lineSpacing = v; }
    public String getMarginsJson() { return marginsJson; }
    public void setMarginsJson(String v) { marginsJson = v; }
    public String getHeadingNumbering() { return headingNumbering; }
    public void setHeadingNumbering(String v) { headingNumbering = v; }
    public String getCitationStyle() { return citationStyle; }
    public void setCitationStyle(String v) { citationStyle = v; }
}
