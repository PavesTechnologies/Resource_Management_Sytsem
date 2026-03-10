package com.entity.skill_entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "skill",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_skill_name_category",
                        columnNames = {"name", "category_id"}
                )
        }
)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

//    @Column(name = "skill_type", nullable = false)
//    private String skillType; // NORMAL | CERTIFICATION

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_skill_category"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "skills"})
    @JsonIgnore
    private SkillCategory category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "skill.category"})
    private List<SubSkill> subSkills = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSubSkill(SubSkill subSkill) {
        subSkills.add(subSkill);
        subSkill.setSkill(this);
    }

    public void removeSubSkill(SubSkill subSkill) {
        subSkills.remove(subSkill);
        subSkill.setSkill(null);
    }
}
