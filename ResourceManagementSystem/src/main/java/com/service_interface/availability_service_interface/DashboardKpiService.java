package com.service_interface.availability_service_interface;

import com.dto.DashboardKpiDTO;
import java.time.LocalDate;

public interface DashboardKpiService {
    
    DashboardKpiDTO calculateKpis(
            LocalDate fromDate,
            LocalDate toDate,
            String role,
            String location,
            String employmentType,
            Integer minExperience,
            Integer maxExperience
    );
}
