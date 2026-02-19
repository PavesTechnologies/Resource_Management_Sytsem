package com.controller.skill_controllers;

import com.dto.ApiResponse;
import com.dto.skill_dto.CertificateRequestDTO;
import com.entity.skill_entities.Skill;
import com.service_interface.skill_service_interface.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService service;

    @PostMapping
    public ResponseEntity<ApiResponse> create(
            @RequestBody CertificateRequestDTO dto) {

        return ResponseEntity.ok(
                ApiResponse.success(service.CreateCertificate(dto))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Skill>>> getAllCertificationSkills() {
        List<Skill> skills = service.getAllCertificationSkills();
        return ResponseEntity.ok(ApiResponse.success("All certification skills retrieved successfully", skills));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Skill>> getCertificationSkillById(@PathVariable UUID id) {
        Skill skill = service.getCertificationSkillById(id);
        return ResponseEntity.ok(ApiResponse.success("Certification skill retrieved successfully", skill));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Skill>>> getCertificationSkillsByCategory(@PathVariable UUID categoryId) {
        List<Skill> skills = service.getCertificationSkillsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Certification skills by category retrieved successfully", skills));
    }
}
