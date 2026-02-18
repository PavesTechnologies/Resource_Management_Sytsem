package com.controller.skill_controllers;


import com.dto.ApiResponse;
import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.ResourceSkill;
import com.service_interface.skill_service_interface.ResourceCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource-certificates")
@RequiredArgsConstructor
public class ResourceCertificateController {
    private final ResourceCertificateService service;

    @PostMapping
    public ResponseEntity<ApiResponse> assignCertification(
            @RequestBody ResourceCertificateRequestDTO dto) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        service.assignCertificate(dto)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceSkill>>> getAllCertificates() {
        List<ResourceSkill> certificates = service.getAllCertificates();
        return ResponseEntity.ok(ApiResponse.success("All certificates retrieved successfully", certificates));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<ApiResponse<List<ResourceSkill>>> getCertificatesByResourceId(@PathVariable Long resourceId) {
        List<ResourceSkill> certificates = service.getCertificatesByResourceId(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Certificates for resource retrieved successfully", certificates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceSkill>> getCertificateById(@PathVariable UUID id) {
        ResourceSkill certificate = service.getCertificateById(id);
        return ResponseEntity.ok(ApiResponse.success("Certificate retrieved successfully", certificate));
    }
}
