package com.entity.client_entities;


import com.entity_enums.client_enums.EnablementAssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_asset_assignment")
public class ClientAssetAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private ClientAsset asset;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;
    // UI fields
    private String resourceName;
    private String projectName;

    private LocalDate assignedDate;
    private LocalDate expectedReturnDate;

    @Column(name="actual_return_date")
    private LocalDate actualReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnablementAssignmentStatus assignmentStatus;

    private String assignedBy;
    private String locationType;
    private String locationDetails;

    @Column(length = 500)
    private String description;

    private Boolean active;
}
