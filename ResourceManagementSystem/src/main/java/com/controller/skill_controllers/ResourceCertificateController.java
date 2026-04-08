package com.controller.skill_controllers;


import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.ResourceCertificate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service_interface.skill_service_interface.ResourceCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
//import org.springframework.web.bind.annotation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource-certificates")
@RequiredArgsConstructor
public class ResourceCertificateController {
    private final ResourceCertificateService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> assignCertification(
            @ModelAttribute ResourceCertificateRequestDTO dto,
            @RequestPart("certificateFile") MultipartFile certificateFile) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        service.assignCertificate(dto, certificateFile)
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

    @PutMapping(value = "/{resourceCertificateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceCertificate>> updateResourceCertificate(
            @PathVariable UUID resourceCertificateId, 
            @RequestPart("certificateData") @Valid ResourceCertificateRequestDTO dto,
            @RequestPart(value = "certificateFile", required = false) MultipartFile certificateFile) {
        ResourceCertificate updated = service.updateResourceCertificate(resourceCertificateId, dto, certificateFile);
        return ResponseEntity.ok(ApiResponse.success("Resource certificate updated successfully", updated));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable UUID id) {
        ResourceCertificate certificate = service.getCertificateById(id);

        if (certificate.getCertificateFile() == null) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(certificate.getCertificateFile());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        certificate.getFileType() != null ? certificate.getFileType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + certificate.getFileName() + "\"")
                .body(resource);
    }
}
