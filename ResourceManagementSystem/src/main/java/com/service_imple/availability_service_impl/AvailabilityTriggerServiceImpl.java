package com.service_imple.availability_service_impl;

import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import com.service_interface.availability_service_interface.AvailabilityTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class AvailabilityTriggerServiceImpl implements AvailabilityTriggerService {

    private final AvailabilityCalculationService calculationService;

    @Override
    @Transactional
    public void triggerMonthlySync(YearMonth yearMonth) {
        try {
            calculationService.calculateForAllResources(yearMonth);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void triggerResourceRecalculation(Long resourceId, YearMonth yearMonth) {
        try {
            calculationService.recalculateForResource(resourceId, yearMonth);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void triggerBulkRecalculation(YearMonth startMonth, YearMonth endMonth) {
        try {
            YearMonth current = startMonth;
            while (!current.isAfter(endMonth)) {
                calculationService.calculateForAllResources(current);
                current = current.plusMonths(1);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void handleHolidayDataChange(Integer year) {
        YearMonth startMonth = YearMonth.of(year, 1);
        YearMonth endMonth = YearMonth.of(year, 12);
        
        triggerBulkRecalculation(startMonth, endMonth);
    }

}