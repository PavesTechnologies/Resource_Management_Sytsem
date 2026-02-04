package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "skill_category",
        uniqueConstraints = @UniqueConstraint(columnNames = "category_name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "active_flag")
    @Builder.Default
    private Boolean activeFlag = true;
}
