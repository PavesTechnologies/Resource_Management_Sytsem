package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "active_flag")
    @Builder.Default
    private Boolean activeFlag = true;
}
