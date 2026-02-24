package com.controller.skill_controllers;

import com.dto.ApiResponse;
import com.dto.skill_dto.AllocationValidationRequestDTO;
import com.service_interface.skill_service_interface.CertificationComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/allocation")
@RequiredArgsConstructor
public class AllocationValidationController {
    private final CertificationComplianceService complianceService;

    @PostMapping(
            value = "/validate",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<ApiResponse<String>> validateAllocation(
            @RequestBody AllocationValidationRequestDTO dto) {
        System.out.println("Resource ID: " + dto.getResourceId());
        System.out.println("Cert List---------------------------------------------------------------------------------------------------------------: " + dto.getRequiredCertificationSkillIds());

        complianceService.validateCertificationCompliance(dto);

        return ResponseEntity.ok(
                ApiResponse.success("Allocation validation passed")
        );
    }

}
