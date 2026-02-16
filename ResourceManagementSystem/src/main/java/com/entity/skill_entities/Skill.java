package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "skill",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "skill_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "skill_id")
    private UUID skillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private SkillCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proficiency_id", nullable = false)
    private ProficiencyLevel proficiencyLevel;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "skill_description")
    private String skillDescription;

    @Column(name = "is_certification")
    @Builder.Default
    private Boolean isCertification = false;

    @Column(name = "active_flag")
    @Builder.Default
    private Boolean activeFlag = true;
}
