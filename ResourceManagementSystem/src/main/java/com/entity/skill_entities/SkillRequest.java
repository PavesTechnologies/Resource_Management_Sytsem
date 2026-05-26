package com.entity.skill_entities;

import com.audit.AuditEntityListener;
import com.entity_enums.skill_enums.SkillRequestStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill_request")
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SkillRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "resource_id", nullable = false, length = 20)
    private String resourceId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "subskill_name", length = 100)
    private String subskillName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proficiency_id",
            foreignKey = @ForeignKey(name = "fk_sr_proficiency"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProficiencyLevel proficiency;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 20)
    private SkillRequestStatus requestStatus = SkillRequestStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 20)
    private String approvedBy;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
