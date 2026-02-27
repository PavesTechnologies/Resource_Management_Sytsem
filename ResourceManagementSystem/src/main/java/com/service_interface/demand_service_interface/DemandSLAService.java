package com.service_interface.demand_service_interface;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface DemandSLAService {

    ResponseEntity<?> getAllDemandSLA();
    ResponseEntity<?> getDemandSLAById(UUID demandId);
}
