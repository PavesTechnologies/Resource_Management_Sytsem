package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.dto.skill_dto.ResourceSkillRequestDTO;
import com.dto.skill_dto.ResourceSubSkillRequestDTO;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceSubSkill;
import com.service_interface.skill_service_interface.ResourceSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource-skills")
@RequiredArgsConstructor
@CrossOrigin
public class ResourceSkillController {

    private final ResourceSkillService resourceSkillService;

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<String>> addSkillsToResource(@Valid @RequestBody ResourceSkillBulkRequestDTO dto) {
        String result = resourceSkillService.addSkillsToResource(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/skill")
    public ResponseEntity<ApiResponse<String>> addSingleSkillToResource(@Valid @RequestBody ResourceSkillRequestDTO dto) {
        String result = resourceSkillService.addSingleSkillToResource(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/subskill")
    public ResponseEntity<ApiResponse<String>> addSingleSubSkillToResource(@Valid @RequestBody ResourceSubSkillRequestDTO dto) {
        String result = resourceSkillService.addSingleSubSkillToResource(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/resource/{resourceId}/skills")
    public ResponseEntity<ApiResponse<List<ResourceSkill>>> getAllResourceSkills(@PathVariable Long resourceId) {
        List<ResourceSkill> skills = resourceSkillService.getAllResourceSkills(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource skills retrieved successfully", skills));
    }

    @GetMapping("/resource/{resourceId}/subskills")
    public ResponseEntity<ApiResponse<List<ResourceSubSkill>>> getAllResourceSubSkills(@PathVariable Long resourceId) {
        List<ResourceSubSkill> subSkills = resourceSkillService.getAllResourceSubSkills(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource sub-skills retrieved successfully", subSkills));
    }

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<ResourceSkill>>> getAllResourceSkills() {
        List<ResourceSkill> skills = resourceSkillService.getAllResourceSkills();
        return ResponseEntity.ok(ApiResponse.success("All resource skills retrieved successfully", skills));
    }

    @GetMapping("/subskills")
    public ResponseEntity<ApiResponse<List<ResourceSubSkill>>> getAllResourceSubSkills() {
        List<ResourceSubSkill> subSkills = resourceSkillService.getAllResourceSubSkills();
        return ResponseEntity.ok(ApiResponse.success("All resource sub-skills retrieved successfully", subSkills));
    }

    @GetMapping("/resource/{resourceId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<List<ResourceSkillProfileResponseDTO>>> getResourceSkillProfile(@PathVariable Long resourceId) {
        List<ResourceSkillProfileResponseDTO> profile = resourceSkillService.getResourceSkillProfile(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource skill profile retrieved successfully", profile));
    }

    
    @PutMapping("/skill/{resourceSkillId}")
    public ResponseEntity<ApiResponse<ResourceSkill>> updateResourceSkill(@PathVariable UUID resourceSkillId, @Valid @RequestBody ResourceSkillRequestDTO dto) {
        ResourceSkill updated = resourceSkillService.updateResourceSkill(resourceSkillId, dto);
        return ResponseEntity.ok(ApiResponse.success("Resource skill updated successfully", updated));
    }

    
    @PutMapping("/subskill/{resourceSubSkillId}")
    public ResponseEntity<ApiResponse<ResourceSubSkill>> updateResourceSubSkill(@PathVariable UUID resourceSubSkillId, @Valid @RequestBody ResourceSubSkillRequestDTO dto) {
        ResourceSubSkill updated = resourceSkillService.updateResourceSubSkill(resourceSubSkillId, dto);
        return ResponseEntity.ok(ApiResponse.success("Resource sub-skill updated successfully", updated));
    }
}
