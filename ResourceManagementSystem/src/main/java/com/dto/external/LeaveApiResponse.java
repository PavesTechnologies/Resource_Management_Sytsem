package com.dto.external;

import lombok.Data;

@Data
public class LeaveApiResponse {
    private Boolean success;
    private String message;
    private EmployeeLeaveData data;
    
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
