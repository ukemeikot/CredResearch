package africa.credresearch.modules.questionnaire.infrastructure.persistence.entity;

import africa.credresearch.common.util.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "survey_responses")
public class SurveyResponseEntity {
    @Id private UUID id;
    @Column(name = "survey_link_id", nullable = false) private UUID surveyLinkId;
    @Column(name = "consent_given", nullable = false) private boolean consentGiven;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "respondent_meta") private String respondentMeta;
    @PrePersist void pre() { if (id == null) id = UuidV7.generate(); if (submittedAt == null) submittedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getSurveyLinkId() { return surveyLinkId; } public void setSurveyLinkId(UUID v) { surveyLinkId = v; }
    public boolean isConsentGiven() { return consentGiven; } public void setConsentGiven(boolean v) { consentGiven = v; }
    public Instant getSubmittedAt() { return submittedAt; } public void setSubmittedAt(Instant v) { submittedAt = v; }
    public String getRespondentMeta() { return respondentMeta; } public void setRespondentMeta(String v) { respondentMeta = v; }
}
