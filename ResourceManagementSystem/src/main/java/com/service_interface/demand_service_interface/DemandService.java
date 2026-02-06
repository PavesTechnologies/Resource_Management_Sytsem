package com.service_interface.demand_service_interface;

import com.dto.ApiResponse;
import com.entity.Demand;
import org.springframework.http.ResponseEntity;

public interface DemandService {
    ResponseEntity<ApiResponse> createDemand(Demand demand);
}
