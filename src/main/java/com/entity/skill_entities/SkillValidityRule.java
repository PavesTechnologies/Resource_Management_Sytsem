package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "skill_validity_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillValidityRule {

    @Id
    @Column(name = "skill_id")
    private UUID skillId;

    @Column(name = "validity_period_months")
    private Integer validityPeriodMonths;

    @Column(name = "recency_threshold_months")
    private Integer recencyThresholdMonths;

    @Column(name = "expiry_required")
    private Boolean expiryRequired;
}
