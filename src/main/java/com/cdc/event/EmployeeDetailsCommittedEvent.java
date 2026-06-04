package com.cdc.event;

public class EmployeeDetailsCommittedEvent {

    private final String employeeId;
    private final String workEmail;

    public EmployeeDetailsCommittedEvent(String employeeId, String workEmail) {
        this.employeeId = employeeId;
        this.workEmail = workEmail;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getWorkEmail() {
        return workEmail;
    }
}
