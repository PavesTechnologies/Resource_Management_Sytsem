package com.service_interface.demand_service_interface;

import com.dto.ApiResponse;
import com.dto.demand_dto.CreateDemandDTO;
import com.dto.demand_dto.UpdateDemandDTO;
import com.entity.demand_entities.Demand;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface DemandService {
//    ResponseEntity<ApiResponse<?>> createDemand(Demand demand);
//    ResponseEntity<ApiResponse<?>> updateDemand(Demand demand);

//    ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto);

    ResponseEntity<ApiResponse<?>> updateDemand(UpdateDemandDTO dto);
    ResponseEntity<ApiResponse<?>> getDemandByProjectId(Long projectId);

    ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto, Long id);
    ResponseEntity<ApiResponse<?>> getDemandById(UUID demandId);
}
