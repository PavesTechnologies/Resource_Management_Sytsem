package com.service_imple.kpi_service_imple;

import com.dto.DashboardKpiDTO;
import com.projection.DashboardKpiProjection;
import com.repo.kpi_repo.DashboardKpiRepository;
import com.service_interface.kpi_service.DashboardKpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardKpiServiceImpl implements DashboardKpiService {

    private final DashboardKpiRepository dashboardKpiRepository;

    @Override
    public DashboardKpiDTO calculateKpis(
            LocalDate fromDate,
            LocalDate toDate,
            String role,
            String location,
            String employmentType,
            Integer minExperience,
            Integer maxExperience) {

        LocalDate defaultFromDate = (fromDate != null) ? fromDate : LocalDate.now();
        LocalDate defaultToDate = (toDate != null) ? toDate : LocalDate.now();

        log.info("Calculating dashboard KPIs for date range: {} to {} with filters: role={}, location={}, employmentType={}, minExperience={}, maxExperience={}",
                defaultFromDate, defaultToDate, role, location, employmentType, minExperience, maxExperience);

        try {
            DashboardKpiProjection result = dashboardKpiRepository.calculateDashboardKpis(
                    defaultFromDate,
                    defaultToDate,
                    role,
                    location,
                    employmentType,
                    minExperience,
                    maxExperience
            );

            return mapProjectionToDTO(result);

        } catch (Exception e) {
            log.error("Error calculating dashboard KPIs", e);
            throw new RuntimeException("Failed to calculate dashboard KPIs", e);
        }
    }

    private DashboardKpiDTO mapProjectionToDTO(DashboardKpiProjection projection) {
        if (projection == null) {
            throw new IllegalStateException("KPI calculation result is null");
        }

        DashboardKpiDTO dto = new DashboardKpiDTO();
        dto.setTotalResources(projection.getTotalResources());
        dto.setFullyAvailable(projection.getFullyAvailable());
        dto.setPartiallyAvailable(projection.getPartiallyAvailable());
        dto.setFullyAllocated(projection.getFullyAllocated());
        dto.setOverAllocated(projection.getOverAllocated());
        dto.setUpcomingAvailability(projection.getUpcomingAvailability());
        dto.setBenchCapacity(projection.getBenchCapacity());
        dto.setUtilization(projection.getUtilization());

        log.info("KPI calculation result - Total: {}, Fully Available: {}, Partially Available: {}, Fully Allocated: {}, Over Allocated: {}, Upcoming Available: {}, Bench Capacity: {}%, Utilization: {}%",
                projection.getTotalResources(),
                projection.getFullyAvailable(),
                projection.getPartiallyAvailable(),
                projection.getFullyAllocated(),
                projection.getOverAllocated(),
                projection.getUpcomingAvailability(),
                projection.getBenchCapacity(),
                projection.getUtilization());
        
        return dto;
    }
}
