package com.service_interface.availability_service_interface;

import java.time.YearMonth;

public interface AvailabilityTriggerService {
    
    void triggerMonthlySync(YearMonth yearMonth);
    
    void triggerResourceRecalculation(Long resourceId, YearMonth yearMonth);
    
    void triggerBulkRecalculation(YearMonth startMonth, YearMonth endMonth);
    
    void handleHolidayDataChange(Integer year);
    
    void handleProjectTimelineChange(Long projectId, java.time.LocalDateTime oldStartDate, java.time.LocalDateTime oldEndDate, 
                                   java.time.LocalDateTime newStartDate, java.time.LocalDateTime newEndDate);
}
