package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "resource_skill",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "skill_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "last_used_date")
    private LocalDate lastUsedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "active_flag")
    private Boolean activeFlag;
}
