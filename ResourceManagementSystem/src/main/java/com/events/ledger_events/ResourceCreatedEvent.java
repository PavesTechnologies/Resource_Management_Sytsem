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
public class ResourceCreatedEvent extends BaseLedgerEvent {

    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;
    private String employmentStatus;
    private String employmentType;
    private String primarySkills;
    private String location;
    private String department;
    private String designation;
    private String createdBy;
    private Map<String, Object> additionalMetadata;

    @Override
    public String generateEventHash() {
        StringBuilder hashInput = new StringBuilder();
        hashInput.append(resourceId)
                .append(employeeId != null ? employeeId : "")
                .append(dateOfJoining != null ? dateOfJoining.toString() : "")
                .append(employmentStatus != null ? employmentStatus : "")
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
        return dateOfJoining != null ? dateOfJoining : LocalDate.now();
    }

    public LocalDate getCalculationEndDate() {
        if (dateOfExit != null) {
            return dateOfExit;
        }
        
        LocalDate horizonEnd = LocalDate.now().plusMonths(12);
        return horizonEnd;
    }

    public boolean isActiveResource() {
        return "ACTIVE".equalsIgnoreCase(employmentStatus) && 
               (dateOfExit == null || dateOfExit.isAfter(LocalDate.now()));
    }

    public LocalDate getEffectiveStartDate() {
        return dateOfJoining != null ? dateOfJoining : LocalDate.now();
    }

    public LocalDate getEffectiveEndDate() {
        if (dateOfExit != null && dateOfExit.isBefore(LocalDate.now().plusMonths(12))) {
            return dateOfExit;
        }
        return LocalDate.now().plusMonths(12);
    }
}
