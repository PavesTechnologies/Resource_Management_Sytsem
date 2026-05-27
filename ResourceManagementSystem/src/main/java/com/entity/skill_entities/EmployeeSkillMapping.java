package com.entity.skill_entities;

import com.audit.AuditEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "employee_skill_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_skill_subskill",
                        columnNames = {"employee_id", "skill_id", "sub_skill_id"}
                )
        }
)
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmployeeSkillMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "employee_id", nullable = false, length = 20)
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_esm_category"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "skills"})
    private SkillCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_esm_skill"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subSkills", "category"})
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_skill_id",
            foreignKey = @ForeignKey(name = "fk_esm_subskill"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "skill"})
    private SubSkill subSkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proficiency_id",
            foreignKey = @ForeignKey(name = "fk_esm_proficiency"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProficiencyLevel proficiency;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
