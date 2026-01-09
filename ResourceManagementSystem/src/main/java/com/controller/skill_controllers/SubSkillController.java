package com.controller.skill_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.entity.skill_entities.SubSkill;
import com.security.CurrentUser;
import com.service_interface.skill_service_interface.SubSkillService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sub-skills")
@RequiredArgsConstructor
public class SubSkillController {

    private final SubSkillService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<SubSkill>> create(
            @RequestParam Long skillId,
            @RequestParam String subSkillName,
            @RequestParam(required = false) String description,
            @RequestParam boolean isCertification,
            @RequestParam(required = false) String proficiencyName,
            @CurrentUser UserDTO user) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Sub-skill created successfully",
                        service.createSubSkill(skillId, subSkillName,
                                description, isCertification,
                                proficiencyName, user))
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE_MANAGER')")
    @GetMapping("/skill/{skillId}")
    public ResponseEntity<ApiResponse<List<SubSkill>>> getBySkill(
            @PathVariable Long skillId) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Sub-skills under skill",
                        service.getActiveSubSkills(skillId))
        );
    }
}
