package com.service_imple.availability_service_impl;

import com.dto.availability_dto.MonthCalculationContext;
import com.dto.external_dto.HolidayDto;
import com.dto.external_dto.LeaveApiResponse;
import com.entity.resource_entities.ResourceAvailabilityLedger;
import com.entity.resource_entities.Resource;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import com.service_interface.external_api_interface.HolidayApiService;
import com.service_interface.external_api_interface.LeaveApiService;
import lombok.RequiredArgsConstructor;
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
public class AvailabilityCalculationServiceImpl implements AvailabilityCalculationService {

    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final HolidayApiService holidayApiService;
    private final LeaveApiService leaveApiService;

    private static final int HOURS_PER_WORKING_DAY = 8;

    /**
     * 🔥 ROLE-OFF INTEGRATION: Calculate monthly availability and update ResourceAvailabilityLedger
     * 
     * This method is called by role-off process through recalculateAvailabilityImmediately()
     * It updates the ResourceAvailabilityLedger with new availability calculations
     * 
     * Role-Off Ledger Updates:
     * - Creates new ledger entry or updates existing one
     * - Updates all availability fields based on current allocations
     * - Sets trust flags and timestamps for data reliability
     * 
     * @param resource The resource to calculate availability for
     * @param yearMonth The month to calculate availability for
     * @return Updated ResourceAvailabilityLedger entry
     */
    @Override
    public ResourceAvailabilityLedger calculateMonthlyAvailability(Resource resource, YearMonth yearMonth) {
        
        MonthCalculationContext context = buildCalculationContext(resource, yearMonth);
        ResourceAvailabilityLedger ledger = calculateLedger(context, resource);
        
        // Check if record exists and update it, otherwise create new one
        Optional<ResourceAvailabilityLedger> existingLedger = ledgerRepository.findByResourceIdAndPeriodStart(
                resource.getResourceId(), context.getPeriodStart());
        
        if (existingLedger.isPresent()) {
            // Update existing record
            ResourceAvailabilityLedger existing = existingLedger.get();
            existing.setPeriodEnd(ledger.getPeriodEnd());
            existing.setStandardHours(ledger.getStandardHours());
            existing.setHolidayHours(ledger.getHolidayHours());
            existing.setLeaveHours(ledger.getLeaveHours());
            existing.setConfirmedAllocHours(ledger.getConfirmedAllocHours());
            existing.setDraftAllocHours(ledger.getDraftAllocHours());
            existing.setTotalAllocation(ledger.getTotalAllocation());
            existing.setAvailablePercentage(ledger.getAvailablePercentage());
            existing.setFirmAvailableHours(ledger.getFirmAvailableHours());
            existing.setProjectedAvailableHours(ledger.getProjectedAvailableHours());
            existing.setAvailabilityTrustFlag(ledger.getAvailabilityTrustFlag());
            existing.setLastCalculatedAt(java.time.LocalDateTime.now());
            return ledgerRepository.save(existing);
        } else {
            // Create new record
            return ledgerRepository.save(ledger);
        }
    }

    @Override
    @Transactional
    public void calculateForAllResources(YearMonth yearMonth) {
                
        List<Resource> allResources = resourceRepository.findAll();
        
        for (Resource resource : allResources) {
            try {
                calculateMonthlyAvailability(resource, yearMonth);
            } catch (Exception e) {
                            }
        }
        
            }

    /**
     * 🔥 ROLE-OFF TRIGGER: Recalculate availability for specific resource and period
     * 
     * This method is called by role-off process through recalculateAvailabilityImmediately()
     * It triggers the availability calculation that updates ResourceAvailabilityLedger
     * 
     * Role-Off Flow:
     * roleOff → closeAllocation → recalculateAvailabilityImmediately → this method
     * 
     * @param resourceId The resource ID (rolled-off resource)
     * @param yearMonth The period to recalculate availability for
     */
    @Override
    public void recalculateForResource(Long resourceId, YearMonth yearMonth) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));
        
        calculateMonthlyAvailability(resource, yearMonth);
        // This updates ResourceAvailabilityLedger with new availability data
    }

    @Override
    public List<ResourceAvailabilityLedger> getAvailabilityForResource(Long resourceId, YearMonth startMonth, YearMonth endMonth) {
        return ledgerRepository.findByResourceIdAndPeriodStartBetweenOrderByPeriodStart(
                resourceId, startMonth.atDay(1), endMonth.atDay(1));
    }

    @Override
    public boolean isCalculationTrustworthy(MonthCalculationContext context) {
        // Consider calculation trustworthy only if all required inputs are available
        // This ensures ledger entries are reliable for decision making
        
        // Must have active resource
        if (!context.isActiveResource()) {
            return false;
        }
        
        // Must have access to external APIs for complete data
        if (!context.isApisHealthy()) {
            return false;
        }
        
        // Must have valid employment status and dates
        if (context.getDateOfJoining() == null) {
            return false;
        }
        
        return true;
    }

    @Override
    public MonthCalculationContext buildCalculationContext(Resource resource, YearMonth yearMonth) {
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        
        boolean holidayApiHealthy = false;
        boolean leaveApiHealthy = false;
        boolean apisHealthy = false;
        boolean isActiveResource = resource.getEmploymentStatus() != EmploymentStatus.EXITED && resource.getActiveFlag();
        
        Set<LocalDate> holidays = Collections.emptySet();
        Set<LocalDate> leaveDates = Collections.emptySet();
        
        // Try Holiday API - MUST be called for complete availability calculation
        try {
            holidayApiHealthy = holidayApiService.isApiHealthy();
            if (holidayApiHealthy) {
                holidays = getHolidaysForMonth(yearMonth.getYear());
            } else {
                // Log warning and continue with empty holidays
                System.err.println("WARNING: Holiday API is unhealthy - availability calculations will be incomplete");
            }
        } catch (Exception e) {
            holidayApiHealthy = false;
            System.err.println("ERROR: Failed to access Holiday API - availability calculations will be incomplete: " + e.getMessage());
        }
        
        // Try Leave API - MUST be called for complete availability calculation
        try {
            leaveApiHealthy = leaveApiService.isApiHealthy();
            if (leaveApiHealthy) {
                leaveDates = getLeaveDatesForEmployee(resource.getResourceId(), yearMonth.getYear(), yearMonth);
            } else {
                // Log warning and continue with empty leave dates
                System.err.println("WARNING: Leave API is unhealthy - availability calculations will be incomplete");
            }
        } catch (Exception e) {
            leaveApiHealthy = false;
            System.err.println("ERROR: Failed to access Leave API - availability calculations will be incomplete: " + e.getMessage());
        }
        
        apisHealthy = holidayApiHealthy && leaveApiHealthy;
        
        if (!apisHealthy) {
            System.err.println("CRITICAL: External APIs are unavailable - availability data may be unreliable");
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
        int totalAllocation = confirmedAllocHours + draftAllocHours;
        
        // Calculate availability percentage (0-100)
        int availablePercentage = standardHours > 0 ? 
                Math.round((float) firmAvailableHours / standardHours * 100) : 0;
        
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
                .totalAllocation(totalAllocation)
                .availablePercentage(availablePercentage)
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
        // MUST call external API for complete availability calculation
        List<HolidayDto> allHolidays = holidayApiService.getHolidaysForYear(year);
        
        if (allHolidays == null) {
            System.err.println("WARNING: Holiday API returned null data - proceeding with no holidays");
            return Collections.emptySet();
        }
        
        return allHolidays.stream()
                .filter(HolidayDto::getIsActive)
                .map(HolidayDto::getHolidayDate)
                .filter(date -> !isWeekend(date)) // Ignore weekend holidays
                .collect(Collectors.toSet());
    }

    private Set<LocalDate> getLeaveDatesForEmployee(Long resourceId, int year, YearMonth targetMonth) throws LeaveApiService.ExternalApiException {
        // MUST call external API for complete availability calculation
        LeaveApiResponse response = leaveApiService.getApprovedLeaveForEmployee(resourceId, year);
        
        LeaveApiResponse.EmployeeLeaveData employeeData = (LeaveApiResponse.EmployeeLeaveData) response.getData();
        
        if (response == null || response.getData() == null || employeeData.getApprovedLeaveDates() == null) {
            System.err.println("WARNING: Leave API returned no data for employee " + resourceId + " - proceeding with no leaves");
            return Collections.emptySet();
        }
        
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        
        return employeeData.getApprovedLeaveDates().stream()
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
                .totalAllocation(0)
                .availablePercentage(0)
                .firmAvailableHours(0)
                .projectedAvailableHours(0)
                .availabilityTrustFlag(false)
                .lastCalculatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private ResourceAvailabilityLedger createBasicLedgerEntry(Resource resource, YearMonth yearMonth) {
                
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        
        // Calculate basic working days (excluding weekends)
        Set<LocalDate> workingDays = getWorkingDaysInRange(periodStart, periodEnd);
        int standardHours = workingDays.size() * HOURS_PER_WORKING_DAY;
        
        // No holidays, leaves, or allocations for basic calculation
        int holidayHours = 0;
        int leaveHours = 0;
        int confirmedAllocHours = 0;
        int draftAllocHours = 0;
        int totalAllocation = confirmedAllocHours + draftAllocHours;
        int firmAvailableHours = standardHours - holidayHours - leaveHours - confirmedAllocHours;
        int projectedAvailableHours = firmAvailableHours - draftAllocHours;
        int availablePercentage = standardHours > 0 ? 
                Math.round((float) firmAvailableHours / standardHours * 100) : 0;
        
        return ResourceAvailabilityLedger.builder()
                .resource(resource)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .standardHours(standardHours)
                .holidayHours(holidayHours)
                .leaveHours(leaveHours)
                .confirmedAllocHours(confirmedAllocHours)
                .draftAllocHours(draftAllocHours)
                .totalAllocation(totalAllocation)
                .availablePercentage(availablePercentage)
                .firmAvailableHours(firmAvailableHours)
                .projectedAvailableHours(projectedAvailableHours)
                .availabilityTrustFlag(false) // Mark as untrustworthy since no external data
                .lastCalculatedAt(java.time.LocalDateTime.now())
                .build();
    }
}
