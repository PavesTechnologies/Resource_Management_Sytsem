package com.service_interface.availability_service_interface;

import java.time.LocalDate;

public interface ResourceTimelineProjection {
    String getId();
    String getFullName();
    String getDesignation();
    String getWorkingLocation();
    Integer getExperiance();
    String getEmploymentType();
    Double getAvgAllocation();
    LocalDate getNoticeStartDate();
    LocalDate getNoticeEndDate();
}
