package com.service_interface.availability_interface;

import java.time.YearMonth;

public interface AvailabilityTriggerService {
    
    void triggerMonthlySync(YearMonth yearMonth);
    
    void triggerResourceRecalculation(Long resourceId, YearMonth yearMonth);
    
    void triggerBulkRecalculation(YearMonth startMonth, YearMonth endMonth);
    
    void handleHolidayDataChange(Integer year);
}
