package com.entity.client_entities;


import com.entity_enums.client_enums.EnablementAssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="resource_enablement_assignment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceEnablementAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    // Which resource
    private Long resourceId;   // employee / contractor ID

    // Which enablement
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private ClientAsset clientAsset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnablementAssignmentStatus status;

    // Optional but very useful
    private String remarks;    // reason for rejection / return

    private LocalDateTime requestedAt;
    private LocalDateTime actionedAt;
}
