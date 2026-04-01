package com.controller.skill_controllers;


import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.ResourceCertificate;
import com.service_interface.skill_service_interface.ResourceCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation;

import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<ResourceCertificate>>> getAllCertificates() {
        List<ResourceCertificate> certificates = service.getAllCertificates();
        return ResponseEntity.ok(ApiResponse.success("All certificates retrieved successfully", certificates));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<ApiResponse<List<ResourceCertificate>>> getCertificatesByResourceId(@PathVariable Long resourceId) {
        List<ResourceCertificate> certificates = service.getCertificatesByResourceId(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Certificates for resource retrieved successfully", certificates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceCertificate>> getCertificateById(@PathVariable UUID id) {
        ResourceCertificate certificate = service.getCertificateById(id);
        return ResponseEntity.ok(ApiResponse.success("Certificate retrieved successfully", certificate));
    }

    @PutMapping("/{resourceCertificateId}")
    public ResponseEntity<ApiResponse<ResourceCertificate>> updateResourceCertificate(@PathVariable UUID resourceCertificateId, @Valid @RequestBody ResourceCertificateRequestDTO dto) {
        ResourceCertificate updated = service.updateResourceCertificate(resourceCertificateId, dto);
        return ResponseEntity.ok(ApiResponse.success("Resource certificate updated successfully", updated));
    }
}
