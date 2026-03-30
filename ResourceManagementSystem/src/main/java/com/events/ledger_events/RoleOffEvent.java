package com.events.ledger_events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RoleOffEvent extends BaseLedgerEvent {

    private Long allocationId;
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
    private Map<String, Object> additionalMetadata;

    @Override
    public String generateEventHash() {
        StringBuilder hashInput = new StringBuilder();
        hashInput.append(resourceId)
                .append(allocationId)
                .append(roleOffDate != null ? roleOffDate.toString() : "")
                .append(allocationEndDate != null ? allocationEndDate.toString() : "")
                .append(roleOffStatus != null ? roleOffStatus : "")
                .append(eventTimestamp != null ? eventTimestamp.toString() : "");
        
        return hashInput.toString().hashCode() + "";
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    public LocalDate getCalculationStartDate() {
        return roleOffDate != null ? roleOffDate : allocationEndDate;
    }

    public LocalDate getCalculationEndDate() {
        return allocationEndDate;
    }

    public boolean isImmediateRoleOff() {
        return roleOffDate != null && 
               (allocationEndDate == null || roleOffDate.isBefore(allocationEndDate));
    }

    public LocalDate getEffectiveEndDate() {
        if (isImmediateRoleOff()) {
            return roleOffDate;
        }
        return allocationEndDate;
    }
}
