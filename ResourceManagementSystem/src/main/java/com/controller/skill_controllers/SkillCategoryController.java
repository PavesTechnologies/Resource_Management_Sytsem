package com.controller.skill_controllers;

import com.dto.client_dto.ApiResponse;
import com.dto.UserDTO;
import com.entity.skill_entities.SkillCategory;
import com.security.CurrentUser;
import com.service_interface.skill_service_interface.SkillCategoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skill-categories")
@RequiredArgsConstructor
public class SkillCategoryController {

    private final SkillCategoryService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<SkillCategory>> create(
            @RequestParam String categoryName,
            @CurrentUser UserDTO user) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Skill category created successfully",
                        service.createCategory(categoryName, user))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillCategory>> update(
            @PathVariable UUID id,
            @RequestParam String categoryName,
            @CurrentUser UserDTO user) {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Skill category updated successfully",
                        service.updateCategory(id, categoryName, user))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @CurrentUser UserDTO user) {

        service.deactivateCategory(id, user);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Skill category deactivated", null)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE_MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillCategory>>> getAll() {

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Active skill categories",
                        service.getAllActiveCategories())
        );
    }
}
