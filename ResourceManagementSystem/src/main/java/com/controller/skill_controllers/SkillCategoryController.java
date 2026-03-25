package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.SkillSearchResultDto;
import com.dto.skill_dto.SkillTaxonomyTreeDto;
import com.entity.skill_entities.SkillCategory;
import com.service_interface.skill_service_interface.SkillCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skill-categories")
@RequiredArgsConstructor
public class SkillCategoryController {

    private final SkillCategoryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<SkillCategory>> create(
            @RequestBody SkillCategory category) {

        SkillCategory created = service.create(
                category.getName(),
                category.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillCategory>>> getAll() {
        List<SkillCategory> categories = service.findAll();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SkillCategory>>> getActiveCategories() {
        List<SkillCategory> categories = service.findActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Active categories retrieved successfully", categories));
    }

    @PostMapping("/{categoryId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(@PathVariable UUID categoryId) {
        service.deactivateCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully"));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<SkillTaxonomyTreeDto>>> getSkillTaxonomyTree() {
        List<SkillTaxonomyTreeDto> treeDto = service.getSkillTaxonomyTree();
        return ResponseEntity.ok(ApiResponse.success("Skill taxonomy tree retrieved successfully", treeDto));
    }

    @GetMapping("/tree/{categoryId}")
    public ResponseEntity<ApiResponse<SkillTaxonomyTreeDto>> getSkillTaxonomyTreeByCategory(@PathVariable UUID categoryId) {
        SkillTaxonomyTreeDto treeDto = service.getSkillTaxonomyTreeByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Skill taxonomy tree retrieved successfully", treeDto));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SkillSearchResultDto>>> searchSkills(
            @RequestParam String query) {
        
        // ========================================================================
        // INPUT VALIDATION
        // ========================================================================
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search query cannot be empty"));
        }

        // ========================================================================
        // SEARCH EXECUTION
        // ========================================================================
        List<SkillSearchResultDto> results = service.searchSkills(query.trim());
        
        // ========================================================================
        // RESPONSE HANDLING
        // ========================================================================
        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("skill not exist"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Skills found successfully", results));
    }
}

