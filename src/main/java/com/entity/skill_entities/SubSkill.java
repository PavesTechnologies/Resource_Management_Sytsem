package com.entity.skill_entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "sub_skill",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subskill_name_skill",
                        columnNames = {"name", "skill_id"}
                )
        }
)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "skill.category"})
public class SubSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sub_skill_id")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subskill_skill"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subSkills", "category"})
    private Skill skill;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
