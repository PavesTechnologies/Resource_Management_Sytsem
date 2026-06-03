package com.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLeaveDto {
    private Long leaveId;
    private Long employeeId;
    private LocalDate leaveDate;
    private String status;
    private String leaveType;
    private LocalDate approvedDate;
}
