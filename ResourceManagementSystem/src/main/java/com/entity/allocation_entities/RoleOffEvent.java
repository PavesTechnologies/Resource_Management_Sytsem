package com.entity.allocation_entities;

import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.demand_enums.ReplacementStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_off_event")
@Data
public class RoleOffEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private DeliveryRoleExpectation role;

    private LocalDate roleOffDate;

    private LocalDate projectEndDate;

    @Enumerated(EnumType.STRING)
    private ReplacementStatus replacementStatus;

    private String skipReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private Long createdBy;
}
