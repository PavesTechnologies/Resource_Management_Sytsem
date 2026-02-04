package com.controller.skill_controllers;

import com.dto.client_dto.ApiResponse;
import com.dto.UserDTO;
import com.entity.skill_entities.Skill;
import com.security.CurrentUser;
import com.service_interface.skill_service_interface.SkillService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService service;

    @PreAuthorize("hasRole('GENERAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<Skill>> create(
            @RequestParam UUID categoryId,
            @RequestParam String skillName,
            @RequestParam(required = false) String description,
            @RequestParam boolean isCertification,
            @RequestParam(required = false) String proficiencyName,
            @CurrentUser UserDTO user) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Skill created successfully",
                        service.createSkill(categoryId, skillName, description,
                                isCertification, proficiencyName, user))
        );
    }

    @PreAuthorize("hasRole('ADMIN,GENERAL')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Skill>> update(
            @PathVariable UUID id,
            @RequestParam String skillName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String proficiencyName,
            @CurrentUser UserDTO user) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Skill updated successfully",
                        service.updateSkill(id, skillName, description,
                                proficiencyName, user))
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE_MANAGER')")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Skill>>> getByCategory(
            @PathVariable UUID categoryId) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Skills under category",
                        service.getActiveSkillsByCategory(categoryId))
        );
    }
}
