package com.entity.bench;

import com.entity_enums.bench.BenchReason;
import com.entity_enums.bench.StateType;
import com.entity_enums.bench.SubState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID resourceStateId;

    @Column(nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StateType stateType;

    @Enumerated(EnumType.STRING)
    private SubState subState;

    private LocalDate benchStartDate;

    @Enumerated(EnumType.STRING)
    private BenchReason benchReason;

    private UUID allocationId;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Builder.Default
    private Boolean currentFlag = true;

    @Column(name = "internal_allocation_percentage")
    private Integer internalAllocationPercentage;

    private String createdBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private Long updatedBy;
    private SubState oldSubStateTypeValue;
    private SubState newSubStateTypeValue;
    private String reason;
}
