package africa.credresearch.modules.questionnaire.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "survey_answers")
public class SurveyAnswerEntity {
    @Id private UUID id;
    @Column(name = "survey_response_id", nullable = false) private UUID surveyResponseId;
    @Column(name = "question_id", nullable = false) private UUID questionId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "value_json") private String valueJson;
    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); }
    public UUID getId() { return id; }
    public UUID getSurveyResponseId() { return surveyResponseId; } public void setSurveyResponseId(UUID v) { surveyResponseId = v; }
    public UUID getQuestionId() { return questionId; } public void setQuestionId(UUID v) { questionId = v; }
    public String getValueJson() { return valueJson; } public void setValueJson(String v) { valueJson = v; }
}
