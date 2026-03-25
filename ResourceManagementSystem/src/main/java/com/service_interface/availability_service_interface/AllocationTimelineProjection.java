package com.service_interface.availability_service_interface;

public interface AllocationTimelineProjection {
    Long getResourceId();
    String getProject();
    java.time.LocalDate getStartDate();
    java.time.LocalDate getEndDate();
    Integer getAllocation();
    String getAllocationStatus();
}
