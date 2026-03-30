package com.service_imple.availability_service_impl;

import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import com.service_interface.availability_service_interface.AvailabilityTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityTriggerServiceImpl implements AvailabilityTriggerService {

    private final AvailabilityCalculationService calculationService;

    @Override
    @Transactional
    public void triggerMonthlySync(YearMonth yearMonth) {
        try {
            calculationService.recalculateForDateRange(null, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        } catch (Exception e) {
            log.error("Monthly sync failed for {}: {}", yearMonth, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void triggerResourceRecalculation(Long resourceId, YearMonth yearMonth) {
        try {
            calculationService.recalculateForDateRange(resourceId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        } catch (Exception e) {
            log.error("Resource recalculation failed for resource {} on {}: {}", resourceId, yearMonth, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void triggerBulkRecalculation(YearMonth startMonth, YearMonth endMonth) {
        try {
            calculationService.recalculateForDateRange(null, startMonth.atDay(1), endMonth.atEndOfMonth());
        } catch (Exception e) {
            log.error("Bulk recalculation failed from {} to {}: {}", startMonth, endMonth, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void handleHolidayDataChange(Integer year) {
        triggerBulkRecalculation(YearMonth.of(year, 1), YearMonth.of(year, 12));
    }

    @Override
    public void handleProjectTimelineChange(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate, LocalDateTime newStartDate, LocalDateTime newEndDate) {
        // Implementation for project timeline change
    }

    @Override
    @Transactional
    public void triggerRangeRecalculation(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            calculationService.recalculateForDateRange(resourceId, startDate, endDate);
        } catch (Exception e) {
            log.error("Range recalculation failed for resource {} from {} to {}: {}", resourceId, startDate, endDate, e.getMessage());
            throw e;
        }
    }
}
