package com.controller.skill_controllers;

import com.dto.client_dto.ApiResponse;
import com.dto.UserDTO;
import com.entity.skill_entities.ResourceSkill;
import com.security.CurrentUser;
import com.service_interface.skill_service_interface.ResourceSkillService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource-skills")
@RequiredArgsConstructor
public class ResourceSkillController {

    private final ResourceSkillService service;

    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ResourceSkill>> addOrUpdate(
            @RequestParam Long resourceId,
            @RequestParam UUID skillId,
            @RequestParam(required = false) LocalDate lastUsedDate,
            @RequestParam(required = false) LocalDate expiryDate,
            @CurrentUser UserDTO user) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Resource skill updated successfully",
                        service.addOrUpdateResourceSkill(
                                resourceId, skillId, lastUsedDate, expiryDate, user))
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE_MANAGER')")
    @GetMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<List<ResourceSkill>>> getSkills(
            @PathVariable Long resourceId) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Active skills for resource",
                        service.getActiveSkillsForResource(resourceId))
        );
    }
}
