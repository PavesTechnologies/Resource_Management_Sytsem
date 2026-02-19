package com.entity.skill_entities;

import com.entity.demand_entities.Demand;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demand_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandRole {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID demandRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private Demand demand;

    @Column(nullable = false)
    private String roleName;

    @Column(nullable = false)
    private Integer minExperienceYears;

    @Column(nullable = false)
    private Integer maxExperienceYears;

    @Column(nullable = false)
    private Long sourceTemplateId;

    @Column(nullable = false)
    private Integer templateVersion;

    @Column(nullable = false)
    private Boolean customized = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "demandRole", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DemandRoleSkill> demandRoleSkills;
}
