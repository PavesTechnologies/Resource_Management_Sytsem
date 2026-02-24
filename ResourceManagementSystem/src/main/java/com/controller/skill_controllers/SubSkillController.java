package com.controller.skill_controllers;

import com.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<SubSkill>> create(@Valid @RequestBody SubSkillCreateDTO subSkillCreateDTO) {

        SubSkill created = service.create(
                subSkillCreateDTO.getSkillId(),
                subSkillCreateDTO.getName(),
                subSkillCreateDTO.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sub-skill created successfully", created));
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

