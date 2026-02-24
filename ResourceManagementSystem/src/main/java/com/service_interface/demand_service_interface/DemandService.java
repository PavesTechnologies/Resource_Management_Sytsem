package com.service_interface.demand_service_interface;

import com.dto.ApiResponse;
import com.entity.demand_entities.Demand;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface DemandService {
    ResponseEntity<ApiResponse<?>> createDemand(Demand demand);
    ResponseEntity<ApiResponse<?>> updateDemand(Demand demand);
    ResponseEntity<ApiResponse<?>> getDemandByProjectId(Long projectId);
    ResponseEntity<ApiResponse<?>> getDemandById(UUID demandId);
    ResponseEntity<ApiResponse<?>> getDemandsByResourceManagerId(Long resourceManagerId);
}
