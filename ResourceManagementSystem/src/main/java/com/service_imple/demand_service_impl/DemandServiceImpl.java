package com.service_imple.demand_service_impl;

import com.dto.ApiResponse;
import com.entity.Demand;
import com.repo.DemandRepository;
import com.service_interface.demand_service_interface.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DemandServiceImpl implements DemandService {

    @Autowired
    private DemandRepository demandRepository;

    @Override
    public ResponseEntity<ApiResponse> createDemand(Demand demand) {
        try {
            // Set default status if not provided
            if (demand.getDemandStatus() == null) {
                demand.setDemandStatus(com.entity_enums.skill_enums.DemandStatus.DRAFT);
            }
            
            // Set created timestamp
            demand.setCreatedAt(LocalDateTime.now());
            
            // Save the demand
            Demand savedDemand = demandRepository.save(demand);
            
            ApiResponse response = new ApiResponse(
                true,
                "Demand created successfully",
                savedDemand.getDemandId()
            );
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (Exception e) {
            ApiResponse response = new ApiResponse(
                false,
                "Failed to create demand: " + e.getMessage(),
                null
            );
            
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
