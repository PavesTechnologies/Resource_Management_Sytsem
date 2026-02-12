package com.service_imple.availability_service_impl.projection;

public interface TimelineKpiProjection {
    Long getTotalResources();
    Long getFullyAvailable();
    Long getPartiallyAvailable();
    Long getFullyAllocated();
    Long getOverAllocated();
    Double getUtilization();
}
