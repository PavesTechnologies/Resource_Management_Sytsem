package com.projection;

public interface DashboardKpiProjection {
    Long getTotalResources();
    Long getFullyAvailable();
    Long getPartiallyAvailable();
    Long getFullyAllocated();
    Long getOverAllocated();
    Long getUpcomingAvailability();
    Integer getBenchCapacity();
    Integer getUtilization();
}
