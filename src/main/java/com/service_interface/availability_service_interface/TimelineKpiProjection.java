package com.service_interface.availability_service_interface;

public interface TimelineKpiProjection {
    Long getTotalResources();
    Long getFullyAvailable();
    Long getPartiallyAvailable();
    Long getFullyAllocated();
    Long getOverAllocated();
        Long getNoticePeriodResources();
    Long getAvailableNoticePeriodResources();
}
