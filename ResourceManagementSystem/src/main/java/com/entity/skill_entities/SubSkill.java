package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "sub_skill",
        uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "sub_skill_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_skill_id")
    private Long subSkillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "sub_skill_name", nullable = false)
    private String subSkillName;

    @Column(name = "sub_skill_description")
    private String subSkillDescription;

    @Column(name = "is_certification")
    @Builder.Default
    private Boolean isCertification = false;

    @Column(name = "active_flag")
    @Builder.Default
    private Boolean activeFlag = true;
}
