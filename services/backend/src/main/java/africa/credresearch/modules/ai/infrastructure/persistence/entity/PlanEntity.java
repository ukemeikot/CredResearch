package africa.credresearch.modules.ai.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String code;
    private String name;
    @Column(name = "ai_monthly_credits", nullable = false) private int aiMonthlyCredits;

    public String getCode() { return code; }
    public int getAiMonthlyCredits() { return aiMonthlyCredits; }
}
