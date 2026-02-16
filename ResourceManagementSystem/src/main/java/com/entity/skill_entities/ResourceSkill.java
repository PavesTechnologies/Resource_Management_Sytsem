package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "resource_skill",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "skill_id", "sub_skill_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "sub_skill_id")
    private UUID subSkillId;

    @Column(name = "proficiency_id", nullable = false)
    private UUID proficiencyId;

    @Column(name = "last_used_date")
    private LocalDate lastUsedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "active_flag")
    private Boolean activeFlag;
}
