package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "resource_subskill",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "sub_skill_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSubSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "sub_skill_id", nullable = false)
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
