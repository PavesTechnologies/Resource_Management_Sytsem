package com.service_imple.availability_impl;

import com.dto.availability.MonthCalculationContext;
import com.dto.external.HolidayDto;
import com.dto.external.LeaveApiResponse;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.resource_entities.Resource;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.availability_interface.AvailabilityCalculationService;
import com.service_interface.external_api_interface.HolidayApiService;
import com.service_interface.external_api_interface.LeaveApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityCalculationServiceImpl implements AvailabilityCalculationService {

    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final HolidayApiService holidayApiService;
    private final LeaveApiService leaveApiService;

    private static final int HOURS_PER_WORKING_DAY = 8;

    @Override
    @Transactional
    public ResourceAvailabilityLedger calculateMonthlyAvailability(Resource resource, YearMonth yearMonth) {
        log.info("Calculating availability for resource {} for month {}", resource.getResourceId(), yearMonth);
        
        MonthCalculationContext context = buildCalculationContext(resource, yearMonth);
        ResourceAvailabilityLedger ledger = calculateLedger(context, resource);
        
        return ledgerRepository.save(ledger);
    }

    @Override
    @Transactional
    public void calculateForAllResources(YearMonth yearMonth) {
        log.info("Starting bulk availability calculation for all resources for month {}", yearMonth);
        
        List<Resource> allResources = resourceRepository.findAll();
        
        for (Resource resource : allResources) {
            try {
                calculateMonthlyAvailability(resource, yearMonth);
            } catch (Exception e) {
                log.error("Failed to calculate availability for resource {} for month {}", 
                        resource.getResourceId(), yearMonth, e);
            }
        }
        
        log.info("Completed bulk availability calculation for {} resources", allResources.size());
    }

    @Override
    @Transactional
    public void recalculateForResource(Long resourceId, YearMonth yearMonth) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));
        
        calculateMonthlyAvailability(resource, yearMonth);
    }

    @Override
    public List<ResourceAvailabilityLedger> getAvailabilityForResource(Long resourceId, YearMonth startMonth, YearMonth endMonth) {
        return ledgerRepository.findByResourceIdAndPeriodStartBetweenOrderByPeriodStart(
                resourceId, startMonth.atDay(1), endMonth.atDay(1));
    }

    @Override
    public boolean isCalculationTrustworthy(MonthCalculationContext context) {
        return context.isApisHealthy() && context.isActiveResource();
    }

    @Override
    public MonthCalculationContext buildCalculationContext(Resource resource, YearMonth yearMonth) {
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        
        boolean apisHealthy = holidayApiService.isApiHealthy() && leaveApiService.isApiHealthy();
        boolean isActiveResource = resource.getEmploymentStatus() != EmploymentStatus.EXITED && resource.getActiveFlag();
        
        Set<LocalDate> holidays = Collections.emptySet();
        Set<LocalDate> leaveDates = Collections.emptySet();
        
        if (apisHealthy) {
            try {
                holidays = getHolidaysForMonth(yearMonth.getYear());
                leaveDates = getLeaveDatesForEmployee(resource.getResourceId(), yearMonth.getYear(), yearMonth);
            } catch (Exception e) {
                log.warn("Failed to fetch external data for resource {} month {}", resource.getResourceId(), yearMonth, e);
                apisHealthy = false;
            }
        }
        
        return MonthCalculationContext.builder()
                .resourceId(resource.getResourceId())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dateOfJoining(resource.getDateOfJoining())
                .dateOfExit(resource.getDateOfExit())
                .holidays(holidays)
                .leaveDates(leaveDates)
                .apisHealthy(apisHealthy)
                .isActiveResource(isActiveResource)
                .build();
    }

    private ResourceAvailabilityLedger calculateLedger(MonthCalculationContext context, Resource resource) {
        LocalDate effectiveStart = getEffectiveStartDate(context);
        LocalDate effectiveEnd = getEffectiveEndDate(context);
        
        if (effectiveStart.isAfter(effectiveEnd)) {
            return createEmptyLedger(context, resource);
        }
        
        Set<LocalDate> workingDays = getWorkingDaysInRange(effectiveStart, effectiveEnd);
        Set<LocalDate> nonWorkingDays = getNonWorkingDays(workingDays, context);
        
        int standardHours = workingDays.size() * HOURS_PER_WORKING_DAY;
        int holidayHours = countHolidayHours(workingDays, context.getHolidays());
        int leaveHours = countLeaveHours(workingDays, context.getLeaveDates(), context.getHolidays());
        
        int confirmedAllocHours = 0; // Phase 1: No allocations
        int draftAllocHours = 0;    // Phase 1: No allocations
        
        int firmAvailableHours = standardHours - holidayHours - leaveHours - confirmedAllocHours;
        int projectedAvailableHours = firmAvailableHours - draftAllocHours;
        
        boolean trustFlag = isCalculationTrustworthy(context);
        
        return ResourceAvailabilityLedger.builder()
                .resource(resource)
                .periodStart(context.getPeriodStart())
                .periodEnd(context.getPeriodEnd())
                .standardHours(standardHours)
                .holidayHours(holidayHours)
                .leaveHours(leaveHours)
                .confirmedAllocHours(confirmedAllocHours)
                .draftAllocHours(draftAllocHours)
                .firmAvailableHours(firmAvailableHours)
                .projectedAvailableHours(projectedAvailableHours)
                .availabilityTrustFlag(trustFlag)
                .lastCalculatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private LocalDate getEffectiveStartDate(MonthCalculationContext context) {
        LocalDate monthStart = context.getPeriodStart();
        LocalDate joiningDate = context.getDateOfJoining();
        
        if (joiningDate != null && joiningDate.isAfter(monthStart)) {
            return joiningDate;
        }
        return monthStart;
    }

    private LocalDate getEffectiveEndDate(MonthCalculationContext context) {
        LocalDate monthEnd = context.getPeriodEnd();
        LocalDate exitDate = context.getDateOfExit();
        
        if (exitDate != null && exitDate.isBefore(monthEnd)) {
            return exitDate;
        }
        return monthEnd;
    }

    private Set<LocalDate> getWorkingDaysInRange(LocalDate start, LocalDate end) {
        return IntStream.range(0, (int) ChronoUnit.DAYS.between(start, end) + 1)
                .mapToObj(start::plusDays)
                .filter(date -> !isWeekend(date))
                .collect(Collectors.toSet());
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private Set<LocalDate> getNonWorkingDays(Set<LocalDate> workingDays, MonthCalculationContext context) {
        Set<LocalDate> nonWorkingDays = new HashSet<>();
        
        nonWorkingDays.addAll(context.getHolidays().stream()
                .filter(workingDays::contains)
                .collect(Collectors.toSet()));
        
        nonWorkingDays.addAll(context.getLeaveDates().stream()
                .filter(workingDays::contains)
                .filter(date -> !context.getHolidays().contains(date)) // Don't double-count holidays
                .collect(Collectors.toSet()));
        
        return nonWorkingDays;
    }

    private int countHolidayHours(Set<LocalDate> workingDays, Set<LocalDate> holidays) {
        return (int) holidays.stream()
                .filter(workingDays::contains)
                .count() * HOURS_PER_WORKING_DAY;
    }

    private int countLeaveHours(Set<LocalDate> workingDays, Set<LocalDate> leaveDates, Set<LocalDate> holidays) {
        return (int) leaveDates.stream()
                .filter(workingDays::contains)
                .filter(date -> !holidays.contains(date)) // Exclude overlapping holidays
                .count() * HOURS_PER_WORKING_DAY;
    }

    private Set<LocalDate> getHolidaysForMonth(int year) throws HolidayApiService.ExternalApiException {
        List<HolidayDto> allHolidays = holidayApiService.getHolidaysForYear(year);
        
        return allHolidays.stream()
                .filter(HolidayDto::getIsActive)
                .map(HolidayDto::getHolidayDate)
                .filter(date -> !isWeekend(date)) // Ignore weekend holidays
                .collect(Collectors.toSet());
    }

    private Set<LocalDate> getLeaveDatesForEmployee(Long resourceId, int year, YearMonth targetMonth) throws LeaveApiService.ExternalApiException {
        LeaveApiResponse response = leaveApiService.getApprovedLeaveForEmployee(resourceId, year);
        
        if (response.getData() == null || response.getData().getApprovedLeaveDates() == null) {
            return Collections.emptySet();
        }
        
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        
        return response.getData().getApprovedLeaveDates().stream()
                .map(LocalDate::parse)
                .filter(date -> !isWeekend(date)) // Ignore weekend leaves
                .filter(date -> !date.isBefore(monthStart) && !date.isAfter(monthEnd)) // Filter to target month only
                .collect(Collectors.toSet());
    }

    private ResourceAvailabilityLedger createEmptyLedger(MonthCalculationContext context, Resource resource) {
        return ResourceAvailabilityLedger.builder()
                .resource(resource)
                .periodStart(context.getPeriodStart())
                .periodEnd(context.getPeriodEnd())
                .standardHours(0)
                .holidayHours(0)
                .leaveHours(0)
                .confirmedAllocHours(0)
                .draftAllocHours(0)
                .firmAvailableHours(0)
                .projectedAvailableHours(0)
                .availabilityTrustFlag(false)
                .lastCalculatedAt(java.time.LocalDateTime.now())
                .build();
    }
}
