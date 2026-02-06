package com.controller.project_controllers;

import com.dto.ApiResponse;
import com.dto.project_dto.DemandRequestDTO;
import com.service_imple.project_service_impl.StaffingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/staffing")
@RequiredArgsConstructor
public class StaffingController {

    private final StaffingService staffingService;

    @PostMapping("/demands")
    public ResponseEntity<ApiResponse<String>> createDemand(@RequestBody DemandRequestDTO request) {
        // Using a random UUID for now as the service's role check is mocked to return true
        UUID dummyUserId = UUID.randomUUID();
        
        staffingService.initiateStaffing(request.getProjectId(), dummyUserId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Demand creation initiated successfully", null));
    }
}
