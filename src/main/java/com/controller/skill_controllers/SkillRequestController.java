package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.BulkSkillApprovalRequestDto;
import com.dto.skill_dto.NewSkillRequestDto;
import com.dto.skill_dto.SkillApprovalDto;
import com.dto.skill_dto.SkillRequestResponseDto;
import com.service_interface.skill_service_interface.SkillRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/skill-taxonomy/requests")
@RequiredArgsConstructor
public class SkillRequestController {

    private final SkillRequestService skillRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> submitSkillRequest(
            @RequestBody NewSkillRequestDto requestDto) {
        String message = skillRequestService.submitSkillRequest(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillRequestResponseDto>>> getAllSkillRequests() {
        List<SkillRequestResponseDto> requests = skillRequestService.getAllSkillRequests();
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", requests));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<List<SkillRequestResponseDto>>> getRequestsByResourceId(
            @PathVariable String resourceId) {
        List<SkillRequestResponseDto> requests = skillRequestService.getRequestsByResourceId(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", requests));
    }

    @PutMapping("/bulk-approve/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkApproveRequests(
            @PathVariable String resourceId,
            @RequestBody BulkSkillApprovalRequestDto dto) {
        Map<String, Integer> result = skillRequestService.bulkApproveByEmployee(resourceId, dto);
        return ResponseEntity.ok(ApiResponse.success("Bulk approval completed successfully", result));
    }

    @PutMapping("/bulk-reject/{resourceId}")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkRejectRequests(
            @PathVariable String resourceId,
            @RequestBody BulkSkillApprovalRequestDto dto) {
        Map<String, Integer> result = skillRequestService.bulkRejectByEmployee(resourceId, dto);
        return ResponseEntity.ok(ApiResponse.success("Bulk rejection completed successfully", result));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveSkillRequest(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "ADMIN") String approvedBy) {
        String message = skillRequestService.approveSkillRequest(id, approvedBy);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectSkillRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) SkillApprovalDto approvalDto) {
        String message = skillRequestService.rejectSkillRequest(id, approvalDto);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
