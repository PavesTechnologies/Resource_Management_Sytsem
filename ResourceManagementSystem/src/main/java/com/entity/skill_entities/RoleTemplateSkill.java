package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "role_template_skill",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"role_template_id", "sub_skill_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleTemplateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_template_id", nullable = false)
    private RoleTemplate roleTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_skill_id", nullable = false)
    private SubSkill subSkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proficiency_id", nullable = false)
    private ProficiencyLevel proficiency;
}

