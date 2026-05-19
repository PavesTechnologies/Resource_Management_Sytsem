package com.service_imple.resource_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.resource.ResourceFiltersDTO;
import com.dto.resource.ResourceNameDTO;
import com.entity.resource_entities.Resource;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.service_interface.resource_service_interface.ResourceEventService;
import com.service_interface.resource_service_interface.ResourceService;
import com.service_interface.roleoff_service_interface.RoleOffService;
import com.service_imple.bench_service_impl.BenchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final ResourceEventService resourceEventService;
    private final ProjectRepository projectRepo;
    private final BenchService benchDetectionService;
    private final RoleOffService roleOffService;

    @Override
    public ResponseEntity<ApiResponse<?>> createResource(Resource resource) {
        if (resource.getFullName() == null || resource.getFullName().trim().isEmpty()) {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "FULL_NAME_REQUIRED", "Full name is required");
        }

        if (resource.getEmail() != null && resourceRepository.existsByEmail(resource.getEmail())) {
            throw new ProjectExceptionHandler(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Email already exists");
        }

        resource.setChangedAt(LocalDateTime.now());
        if (resource.getActiveFlag() == null) {
            resource.setActiveFlag(true);
        }
        if (resource.getEmploymentStatus() == null) {
            resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
        }

        Resource savedResource = resourceRepository.save(resource);
        benchDetectionService.initializeResourceState(savedResource.getResourceId());
        resourceEventService.triggerLedgerCalculationAfterCreate(savedResource.getResourceId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created successfully", savedResource.getResourceId()));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getResourceById(String resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found"));
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", resource));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getResourceByEmployeeCode(String employeeCode) {
        throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "EMPLOYEE_CODE_UNSUPPORTED",
                "Resource lookup by employee code is not supported");
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateResource(Resource resource) {
        Resource existing = resourceRepository.findById(resource.getResourceId())
                .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found"));

        if (resource.getEmail() != null
                && !resource.getEmail().equals(existing.getEmail())
                && resourceRepository.existsByEmail(resource.getEmail())) {
            throw new ProjectExceptionHandler(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Email already exists");
        }

        if (resource.getDateOfExit() != null && EmploymentStatus.ON_NOTICE.equals(resource.getEmploymentStatus())) {
            log.info("Attrition detected for resource: {}. Triggering attrition flow.", resource.getResourceId());
            roleOffService.handleAttrition(resource.getResourceId(), resource.getDateOfExit(), 0L);
        }

        resourceRepository.save(resource);
        resourceEventService.triggerLedgerCalculationAfterUpdate(resource.getResourceId());
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", existing.getResourceId()));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteResource(String resourceId) {
        Resource existing = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found"));

        ledgerRepository.deleteByResourceId(resourceId);
        resourceRepository.delete(existing);
        resourceEventService.triggerLedgerCleanupAfterDelete(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", existing));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllResources() {
        List<String> uniqueLocations = resourceRepository.findDistinctLocations();
        List<String> uniqueDesignations = resourceRepository.findDistinctDesignations();
        Long maxExperience = resourceRepository.findMaxExperience();
        List<String> projectNames = projectRepo.findProjectNamesExceptStatus(ProjectStatus.COMPLETED);
        ResourceFiltersDTO dto = new ResourceFiltersDTO(uniqueLocations, uniqueDesignations, maxExperience, projectNames);
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", dto));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getResources() {
        List<ResourceNameDTO> resources = resourceRepository.findAll().stream()
                .map(resource -> new ResourceNameDTO(resource.getFullName(), resource.getResourceId(), resource.getDesignation()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", resources));
    }
}
