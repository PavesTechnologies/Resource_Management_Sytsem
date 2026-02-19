package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "demand_role_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandRoleSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_role_id", nullable = false)
    private DemandRole demandRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_skill_id", nullable = false)
    private SubSkill subSkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proficiency_id", nullable = false)
    private ProficiencyLevel proficiency;
}

