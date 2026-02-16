package com.controller.skill_controllers;

import com.dto.ApiResponse;
import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.service_interface.skill_service_interface.ResourceSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource-skills")
@RequiredArgsConstructor
public class ResourceSkillController {

    private final ResourceSkillService resourceSkillService;

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<String>> addSkillsToResource(@Valid @RequestBody ResourceSkillBulkRequestDTO dto) {
        String result = resourceSkillService.addSkillsToResource(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/resource/{resourceId}/profile")
    public ResponseEntity<ApiResponse<List<ResourceSkillProfileResponseDTO>>> getResourceSkillProfile(@PathVariable Long resourceId) {
        List<ResourceSkillProfileResponseDTO> profile = resourceSkillService.getResourceSkillProfile(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource skill profile retrieved successfully", profile));
    }
}
