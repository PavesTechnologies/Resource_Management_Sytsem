package com.service_interface.availability_interface;

import com.dto.availability.MonthCalculationContext;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.resource_entities.Resource;

import java.time.YearMonth;
import java.util.List;

public interface AvailabilityCalculationService {
    
    ResourceAvailabilityLedger calculateMonthlyAvailability(Resource resource, YearMonth yearMonth);
    
    void calculateForAllResources(YearMonth yearMonth);
    
    void recalculateForResource(Long resourceId, YearMonth yearMonth);
    
    List<ResourceAvailabilityLedger> getAvailabilityForResource(Long resourceId, YearMonth startMonth, YearMonth endMonth);
    
    boolean isCalculationTrustworthy(MonthCalculationContext context);
    
    MonthCalculationContext buildCalculationContext(Resource resource, YearMonth yearMonth);
}
