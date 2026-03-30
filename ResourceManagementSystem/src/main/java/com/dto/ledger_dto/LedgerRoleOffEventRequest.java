package com.dto.ledger_dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LedgerRoleOffEventRequest {
    private String eventId;
    private Long allocationId;
    private Long resourceId;
    private Long projectId;
    private Long demandId;
    private LocalDate roleOffDate;
    private LocalDate allocationEndDate;
    private String roleOffReason;
    private String roleOffStatus;
    private String roleOffInitiatedBy;
    private String roleOffComments;
    private Boolean isProjectClosure;
    private LocalDate projectEndDate;
}
