package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.SubSkillCreateDTO;
import com.entity.skill_entities.SubSkill;
import com.service_interface.skill_service_interface.SubSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sub-skills")
@RequiredArgsConstructor
public class SubSkillController {

    private final SubSkillService service;

    @PostMapping
    public ResponseEntity<ApiResponse<List<SubSkill>>> create(@Valid @RequestBody SubSkillCreateDTO subSkillCreateDTO) {

        List<SubSkill> created = service.createMultiple(
                subSkillCreateDTO.getSkillId(),
                subSkillCreateDTO.getSubSkills()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created.size() + " sub-skills created successfully", created));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SubSkill>>> getActiveSubSkills() {
        List<SubSkill> subSkills = service.findActiveSubSkills();
        return ResponseEntity.ok(ApiResponse.success("Active sub-skills retrieved successfully", subSkills));
    }

    @GetMapping("/skill/{skillId}")
    public ResponseEntity<ApiResponse<List<SubSkill>>> getActiveSubSkillsBySkill(@PathVariable UUID skillId) {
        List<SubSkill> subSkills = service.findActiveSubSkillsBySkillId(skillId);
        return ResponseEntity.ok(ApiResponse.success("Sub-skills retrieved successfully", subSkills));
    }

    @PostMapping("/{subSkillId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateSubSkill(@PathVariable UUID subSkillId) {
        service.deactivateSubSkill(subSkillId);
        return ResponseEntity.ok(ApiResponse.success("Sub-skill deactivated successfully"));
    }
}

