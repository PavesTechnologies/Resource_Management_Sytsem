package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.entity.skill_entities.Skill;
import com.service_interface.skill_service_interface.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Skill>> create(@RequestBody Skill skill) {

        if (skill.getCategory() == null || skill.getCategory().getId() == null) {
            throw new RuntimeException("Category ID is required");
        }

//        if (skill.getSkillType() == null) {
//            throw new RuntimeException("Skill type is required (NORMAL or CERTIFICATION)");
//        }

        Skill created = service.create(
                skill.getCategory().getId(),
                skill.getName(),
                skill.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Skill created successfully", created));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Skill>>> getActiveSkills() {
        List<Skill> skills = service.findActiveSkills();
        return ResponseEntity.ok(ApiResponse.success("Active skills retrieved successfully", skills));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Skill>>> getActiveSkillsByCategory(@PathVariable UUID categoryId) {
        List<Skill> skills = service.findActiveSkillsByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Skills retrieved successfully", skills));
    }

    @PostMapping("/{skillId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateSkill(@PathVariable UUID skillId) {
        service.deactivateSkill(skillId);
        return ResponseEntity.ok(ApiResponse.success("Skill deactivated successfully"));
    }
}

