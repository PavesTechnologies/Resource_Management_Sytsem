package com.entity.skill_entities;

//import com.entity_enums.skill_enums.AppliesToType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "skill_requirement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "requirement_id")
    private UUID requirementId;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "applies_to_type", nullable = false)
//    private AppliesToType appliesToType;

    @Column(name = "applies_to_id", nullable = false)
    private Long appliesToId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "proficiency_name")
    private String proficiencyName;

    @Column(name = "mandatory_flag")
    private Boolean mandatoryFlag;
}
