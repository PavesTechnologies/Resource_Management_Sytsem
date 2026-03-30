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
public class AllocationChangedEvent extends BaseLedgerEvent {

    private Long allocationId;
    private Long projectId;
    private Long demandId;
    private AllocationChangeType changeType;
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
    private Map<String, Object> additionalMetadata;

    public enum AllocationChangeType {
        CREATED,
        UPDATED,
        DELETED,
        STATUS_CHANGED,
        PERCENTAGE_CHANGED,
        TIMELINE_CHANGED,
        RESOURCE_CHANGED
    }

    @Override
    public String generateEventHash() {
        StringBuilder hashInput = new StringBuilder();
        hashInput.append(resourceId)
                .append(effectiveStartDate != null ? effectiveStartDate.toString() : "")
                .append(effectiveEndDate != null ? effectiveEndDate.toString() : "")
                .append(changeType.name())
                .append(newAllocationPercentage != null ? newAllocationPercentage : "")
                .append(allocationStatus != null ? allocationStatus : "")
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
        if (changeType == AllocationChangeType.DELETED) {
            return previousStartDate != null ? previousStartDate : effectiveStartDate;
        }
        
        LocalDate start = effectiveStartDate;
        if (previousStartDate != null && (start == null || previousStartDate.isBefore(start))) {
            start = previousStartDate;
        }
        
        return start;
    }

    public LocalDate getCalculationEndDate() {
        if (changeType == AllocationChangeType.DELETED) {
            return previousEndDate != null ? previousEndDate : effectiveEndDate;
        }
        
        LocalDate end = effectiveEndDate;
        if (previousEndDate != null && (end == null || previousEndDate.isAfter(end))) {
            end = previousEndDate;
        }
        
        return end;
    }
}
