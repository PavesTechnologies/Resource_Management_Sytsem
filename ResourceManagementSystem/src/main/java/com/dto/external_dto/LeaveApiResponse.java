package com.dto.external_dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

@Data
public class LeaveApiResponse {
    private Boolean success;
    private String message;
    private Object data; // Can be either List<EmployeeLeaveData> or EmployeeLeaveData
    
    public boolean isSuccess() {
        return success != null && success;
    }

    @Data
    public static class EmployeeLeaveData {
        private String employeeId;
        private String employeeName;
        private java.util.List<String> approvedLeaveDates;
    }
}
