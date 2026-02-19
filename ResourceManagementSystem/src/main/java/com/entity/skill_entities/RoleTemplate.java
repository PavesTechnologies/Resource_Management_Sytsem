package com.entity.skill_entities;

import com.entity_enums.skill_enums.TemplateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "role_template",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"role_name", "version"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID roleTemplateId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "min_experience_years", nullable = false)
    private Integer minExperienceYears;

    @Column(name = "max_experience_years", nullable = false)
    private Integer maxExperienceYears;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status;

    @Column(nullable = false)
    private Boolean activeFlag = true;

    @Column(nullable = false)
    private Boolean locked = false;
    // Prevent direct editing once used

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String createdBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String updatedBy;

    @OneToMany(mappedBy = "roleTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoleTemplateSkill> templateSkills;
}
