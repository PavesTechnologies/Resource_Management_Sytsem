package com.dto.ledger_dto;

import com.events.ledger_events.AllocationChangedEvent;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LedgerAllocationChangedRequest {
    private String eventId;
    private Long allocationId;
    private Long resourceId;
    private Long projectId;
    private Long demandId;
    private AllocationChangedEvent.AllocationChangeType changeType;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private LocalDate previousStartDate;
    private LocalDate previousEndDate;
    private Integer previousAllocationPercentage;
    private Integer newAllocationPercentage;
    private String allocationStatus;
    private String previousAllocationStatus;
    private String changedBy;
    private String changeReason;
}
