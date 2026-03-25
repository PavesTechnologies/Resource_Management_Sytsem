package com.service_imple.availability_service_impl;

import com.dto.availability_dto.DashboardKpiDTO;
import com.service_interface.availability_service_interface.DashboardKpiProjection;
import com.repo.resource_repo.DashboardKpiRepository;
import com.service_interface.availability_service_interface.DashboardKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
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
        
        return dto;
    }
}
