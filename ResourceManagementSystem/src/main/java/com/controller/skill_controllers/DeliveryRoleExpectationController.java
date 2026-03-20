package com.controller.skill_controllers;

import com.dto.ApiResponse;
import com.dto.skill_dto.DeliveryRoleExpectationRequest;
import com.dto.skill_dto.DeliveryRoleExpectationResponse;
import com.dto.skill_dto.RoleExpectationRequest;
import com.dto.skill_dto.RoleExpectationWithMandatoryResponse;
import com.dto.skill_dto.RoleListResponse;
import com.exception.skill_exceptions.DuplicateRoleExpectationException;
import com.exception.skill_exceptions.SkillValidationException;
import com.service_interface.skill_service_interface.DeliveryRoleExpectationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/role-expectations")
@RequiredArgsConstructor
@Slf4j
public class DeliveryRoleExpectationController {

    private final DeliveryRoleExpectationService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROJECT-MANAGER', 'DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryRoleExpectationResponse>> createRoleExpectations(
            @Valid @RequestBody DeliveryRoleExpectationRequest request) {
        
        log.info("Admin request to create role expectations for role: {}", request.getRoleName());
        
        try {
            DeliveryRoleExpectationResponse response = service.createRoleExpectations(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Role expectations created successfully", response));
        } catch (SkillValidationException e) {
            log.warn("Validation error for role expectations: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (DuplicateRoleExpectationException e) {
            log.warn("Duplicate role expectation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating role expectations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryRoleExpectationResponse>> updateRoleExpectations(
            @PathVariable UUID roleId,
            @Valid @RequestBody DeliveryRoleExpectationRequest request) {
        
        log.info("Admin request to update role expectations for roleId: {}", roleId);
        
        try {
            DeliveryRoleExpectationResponse response = service.updateRoleExpectations(roleId, request);
            return ResponseEntity.ok(ApiResponse.success("Role expectations updated successfully", response));
        } catch (SkillValidationException e) {
            log.warn("Validation error for role expectations: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (DuplicateRoleExpectationException e) {
            log.warn("Duplicate role expectation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating role expectations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<DeliveryRoleExpectationResponse>> createOrUpdateRoleExpectations(
//            @Valid @RequestBody DeliveryRoleExpectationRequest request) {
//
//        log.info("Admin request to create/update role expectations for role: {}", request.getRoleName());
//
//        try {
//            DeliveryRoleExpectationResponse response = service.createOrUpdateRoleExpectations(request);
//            return ResponseEntity.ok(ApiResponse.success("Role expectations created/updated successfully", response));
//        } catch (SkillValidationException e) {
//            log.warn("Validation error for role expectations: {}", e.getMessage());
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        } catch (DuplicateRoleExpectationException e) {
//            log.warn("Duplicate role expectation: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
//        } catch (Exception e) {
//            log.error("Unexpected error creating/updating role expectations", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Internal server error"));
//        }
//    }

    @GetMapping("/{roleName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER', 'MANAGER', 'RESOURCE-USER')")
    public ResponseEntity<ApiResponse<DeliveryRoleExpectationResponse>> getRoleExpectations(
            @PathVariable String roleName) {
        
        log.info("Admin request to get expectations for role: {}", roleName);
        
        try {
            DeliveryRoleExpectationResponse response = service.getRoleExpectations(roleName);
            return ResponseEntity.ok(ApiResponse.success("Role expectations retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error retrieving role expectations for role: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryRoleExpectationResponse>>> getAllRoleExpectations() {
        
        log.info("Admin request to get all role expectations");
        
        try {
            List<DeliveryRoleExpectationResponse> responses = service.getAllRoleExpectations();
            return ResponseEntity.ok(ApiResponse.success("All role expectations retrieved successfully", responses));
        } catch (Exception e) {
            log.error("Error retrieving all role expectations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/{roleName}/mandatory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleExpectationWithMandatoryResponse>> getRoleExpectationsWithMandatory(
            @PathVariable String roleName) {
        
        log.info("Admin request to get role expectations with mandatory/optional separation for role: {}", roleName);
        
        try {
            RoleExpectationWithMandatoryResponse response = service.getRoleExpectationsWithMandatory(roleName);
            return ResponseEntity.ok(ApiResponse.success("Role expectations with mandatory/optional separation retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error retrieving role expectations with mandatory/optional separation for role: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/{roleName}/eligibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkResourceEligibility(
            @PathVariable String roleName,
            @RequestParam List<UUID> resourceSkillIds) {
        
        log.info("Admin request to check resource eligibility for role: {}", roleName);
        
        try {
            boolean isEligible = service.isResourceEligibleForRole(roleName, resourceSkillIds);
            return ResponseEntity.ok(ApiResponse.success("Resource eligibility check completed", isEligible));
        } catch (Exception e) {
            log.error("Error checking resource eligibility for role: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleListResponse>> getAvailableRoles() {
        
        log.info("Admin request to get available roles");
        
        try {
            RoleListResponse response = service.getAvailableRoles();
            return ResponseEntity.ok(ApiResponse.success("Available roles retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error retrieving available roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @DeleteMapping("/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRoleExpectations(
            @PathVariable String roleName) {
        
        log.info("Admin request to soft delete expectations for role: {}", roleName);
        
        try {
            service.deleteRoleExpectations(roleName);
            return ResponseEntity.ok(ApiResponse.success("Role expectations soft deleted successfully"));
        } catch (Exception e) {
            log.error("Error soft deleting role expectations for role: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/{roleName}/exists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasRoleExpectations(
            @PathVariable String roleName) {
        
        log.info("Admin request to check if role has expectations: {}", roleName);
        
        try {
            boolean hasExpectations = service.hasRoleExpectations(roleName);
            return ResponseEntity.ok(ApiResponse.success("Role expectation check completed", hasExpectations));
        } catch (Exception e) {
            log.error("Error checking role expectations for role: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}
