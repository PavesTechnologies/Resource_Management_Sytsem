package com.dto.ledger_dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LedgerResourceCreatedRequest {
    private String eventId;
    private Long resourceId;
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
}
