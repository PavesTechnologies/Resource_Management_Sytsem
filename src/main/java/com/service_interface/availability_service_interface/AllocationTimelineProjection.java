package com.service_interface.availability_service_interface;

import java.time.LocalDate;

public interface AllocationTimelineProjection {
    String getResourceId();
    String getProject();
    LocalDate getStartDate();
    LocalDate getEndDate();
    Integer getAllocation();
    String getAllocationStatus();
}
