package com.service_imple.availability_service_impl.projection;

import java.time.LocalDate;

public interface ResourceTimelineProjection {
    Long getId();
    String getFullName();
    String getDesignation();
    String getWorkingLocation();
    Integer getExperiance();
    String getEmploymentType();
    Double getAvgAllocation();
    LocalDate getNoticeStartDate();
    LocalDate getNoticeEndDate();
    Boolean getAllocationAllowed();
}
