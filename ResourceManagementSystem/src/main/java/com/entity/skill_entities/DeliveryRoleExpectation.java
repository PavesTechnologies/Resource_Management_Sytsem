package com.entity.skill_entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "delivery_role_expectation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_skill_subskill",
                        columnNames = {"role_name", "skill_id", "sub_skill_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DeliveryRoleExpectation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_expectation_skill"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subSkills", "category"})
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_skill_id",
            foreignKey = @ForeignKey(name = "fk_expectation_subskill"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "skill"})
    private SubSkill subSkill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proficiency_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_expectation_proficiency"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProficiencyLevel proficiencyLevel;

    @Column(name = "mandatory_flag", nullable = false)
    private Boolean mandatoryFlag;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
