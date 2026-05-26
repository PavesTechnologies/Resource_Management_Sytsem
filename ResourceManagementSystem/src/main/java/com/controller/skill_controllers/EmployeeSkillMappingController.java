package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.EmployeeSkillResponseDto;
import com.dto.skill_dto.EmployeeSkillsRequestDto;
import com.service_interface.skill_service_interface.EmployeeSkillMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-skills")
@RequiredArgsConstructor
public class EmployeeSkillMappingController {

    private final EmployeeSkillMappingService employeeSkillMappingService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> saveSkillMappings(
            @RequestBody EmployeeSkillsRequestDto requestDto) {
        String message = employeeSkillMappingService.saveSkillMappings(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<List<EmployeeSkillResponseDto>>> getEmployeeSkills(
            @PathVariable String resourceId) {
        List<EmployeeSkillResponseDto> skills = employeeSkillMappingService.getEmployeeSkills(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", skills));
    }
}
