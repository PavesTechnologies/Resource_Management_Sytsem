package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "proficiency_level",
        uniqueConstraints = @UniqueConstraint(columnNames = "proficiency_code")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProficiencyLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "proficiency_id")
    private UUID proficiencyId;

    @Column(name = "proficiency_code", nullable = false)
    private String proficiencyCode;

    @Column(name = "proficiency_name", nullable = false)
    private String proficiencyName;


    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "active_flag")
    @Builder.Default
    private Boolean activeFlag = true;
}
