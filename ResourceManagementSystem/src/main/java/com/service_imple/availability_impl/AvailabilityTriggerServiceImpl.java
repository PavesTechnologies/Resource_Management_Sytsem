package com.service_imple.availability_impl;

import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.availability_interface.AvailabilityCalculationService;
import com.service_interface.availability_interface.AvailabilityTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityTriggerServiceImpl implements AvailabilityTriggerService {

    private final AvailabilityCalculationService calculationService;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public void triggerMonthlySync(YearMonth yearMonth) {
        log.info("Starting monthly availability sync for {}", yearMonth);
        
        try {
            calculationService.calculateForAllResources(yearMonth);
            log.info("Successfully completed monthly sync for {}", yearMonth);
        } catch (Exception e) {
            log.error("Failed to complete monthly sync for {}", yearMonth, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void triggerResourceRecalculation(Long resourceId, YearMonth yearMonth) {
        log.info("Triggering recalculation for resource {} for month {}", resourceId, yearMonth);
        
        try {
            calculationService.recalculateForResource(resourceId, yearMonth);
            log.info("Successfully recalculated availability for resource {} month {}", resourceId, yearMonth);
        } catch (Exception e) {
            log.error("Failed to recalculate availability for resource {} month {}", resourceId, yearMonth, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void triggerBulkRecalculation(YearMonth startMonth, YearMonth endMonth) {
        log.info("Starting bulk recalculation from {} to {}", startMonth, endMonth);
        
        try {
            YearMonth current = startMonth;
            while (!current.isAfter(endMonth)) {
                calculationService.calculateForAllResources(current);
                current = current.plusMonths(1);
            }
            
            log.info("Successfully completed bulk recalculation from {} to {}", startMonth, endMonth);
        } catch (Exception e) {
            log.error("Failed to complete bulk recalculation from {} to {}", startMonth, endMonth, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void handleHolidayDataChange(Integer year) {
        log.info("Handling holiday data change for year {}", year);
        
        YearMonth startMonth = YearMonth.of(year, 1);
        YearMonth endMonth = YearMonth.of(year, 12);
        
        triggerBulkRecalculation(startMonth, endMonth);
    }

}
