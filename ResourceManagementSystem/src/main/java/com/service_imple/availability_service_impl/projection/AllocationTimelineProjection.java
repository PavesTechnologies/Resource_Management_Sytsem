package com.service_imple.availability_service_impl.projection;

public interface AllocationTimelineProjection {
    Long getResourceId();
    String getProject();
    java.time.LocalDate getStartDate();
    java.time.LocalDate getEndDate();
    Integer getAllocation();
    String getAllocationStatus();
}
