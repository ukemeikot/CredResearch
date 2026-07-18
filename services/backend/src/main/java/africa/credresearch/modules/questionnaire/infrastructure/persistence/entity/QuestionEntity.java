package africa.credresearch.modules.questionnaire.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "questions")
public class QuestionEntity {
    @Id private UUID id;
    @Column(name = "questionnaire_id", nullable = false) private UUID questionnaireId;
    @Column(name = "order_index", nullable = false) private int orderIndex;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String prompt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "options_json") private String optionsJson;
    @Column(nullable = false) private boolean required;
    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); }
    public UUID getId() { return id; }
    public UUID getQuestionnaireId() { return questionnaireId; } public void setQuestionnaireId(UUID v) { questionnaireId = v; }
    public int getOrderIndex() { return orderIndex; } public void setOrderIndex(int v) { orderIndex = v; }
    public String getType() { return type; } public void setType(String v) { type = v; }
    public String getPrompt() { return prompt; } public void setPrompt(String v) { prompt = v; }
    public String getOptionsJson() { return optionsJson; } public void setOptionsJson(String v) { optionsJson = v; }
    public boolean isRequired() { return required; } public void setRequired(boolean v) { required = v; }
}
