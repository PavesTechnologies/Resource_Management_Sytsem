package com.service_interface.demand_service_interface;

import com.dto.centralised_dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface DemandSLAService {

    ResponseEntity<ApiResponse<?>> getAllDemandSLA();
    ResponseEntity<ApiResponse<?>> getDemandSLAById(UUID demandId);
}
