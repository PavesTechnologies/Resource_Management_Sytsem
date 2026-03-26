package com.service_imple.resource_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.resource.ResourceFiltersDTO;
import com.dto.resource.ResourceNameDTO;
import com.entity.resource_entities.Resource;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.WorkforceCategory;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import com.service_interface.resource_service_interface.ResourceEventService;
import com.service_interface.resource_service_interface.ResourceService;
import com.service_imple.bench_service_impl.BenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AvailabilityCalculationService availabilityCalculationService;

    @Autowired
    private ResourceAvailabilityLedgerRepository ledgerRepository;

    @Autowired
    private ResourceEventService resourceEventService;

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private BenchService benchDetectionService;

    @Override
    public ResponseEntity<ApiResponse<?>> createResource(Resource resource) {
        try {
            // Validate required fields

            if (resource.getFullName() == null || resource.getFullName().trim().isEmpty()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "FULL_NAME_REQUIRED",
                        "Full name is required"
                );
            }

            if (resource.getEmploymentType() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "EMPLOYMENT_TYPE_REQUIRED",
                        "Employment type is required"
                );
            }

            if (resource.getWorkforceCategory() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "WORKFORCE_CATEGORY_REQUIRED",
                        "Workforce category is required"
                );
            }

            // Check for duplicate employee code


            // Check for duplicate email
            if (resource.getEmail() != null && resourceRepository.existsByEmail(resource.getEmail())) {
                throw new ProjectExceptionHandler(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_EMAIL",
                        "Email already exists"
                );
            }

            // Set default values
            if (resource.getActiveFlag() == null) {
                resource.setActiveFlag(true);
            }

            if (resource.getEmploymentStatus() == null) {
                resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
            }

            if (resource.getAllocationAllowed() == null) {
                resource.setAllocationAllowed(true);
            }

            // Set audit fields
            resource.setChangedAt(LocalDateTime.now());
            resource.setHrLastSyncedAt(LocalDateTime.now());

            // Save the resource
            Resource savedResource = resourceRepository.save(resource);
            
            // Initialize resource state (automatic bench/project state creation)
            benchDetectionService.initializeResourceState(savedResource.getResourceId());
            
            // Trigger async availability ledger calculation
            resourceEventService.triggerLedgerCalculationAfterCreate(savedResource.getResourceId());


            ApiResponse response = new ApiResponse(
                    true,
                    "Resource created successfully",
                    savedResource.getResourceId()
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (ProjectExceptionHandler e) {
            // Business validation failure
            ApiResponse response = new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, e.getStatus());

        } catch (Exception e) {
            // Unexpected failure
            ApiResponse response = new ApiResponse(
                    false,
                    "Failed to create resource: " + e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getResourceById(Long resourceId) {
        try {
            if (resourceId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "RESOURCE_ID_REQUIRED",
                        "Resource ID is required"
                );
            }

            Resource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "RESOURCE_NOT_FOUND",
                            "Resource not found"
                    ));

            ApiResponse response = new ApiResponse(
                    true,
                    "Resource retrieved successfully",
                    resource
            );

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (ProjectExceptionHandler e) {
            ApiResponse response = new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, e.getStatus());

        } catch (Exception e) {
            ApiResponse response = new ApiResponse(
                    false,
                    "Failed to retrieve resource: " + e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getResourceByEmployeeCode(String employeeCode) {
        return null;
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateResource(Resource resource) {
        try {
            if (resource.getResourceId() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "RESOURCE_ID_REQUIRED",
                        "Resource ID is required for update"
                );
            }

            Resource existing = resourceRepository.findById(resource.getResourceId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "RESOURCE_NOT_FOUND",
                            "Resource not found"
                    ));


            // Check for duplicate email if being updated
            if (resource.getEmail() != null && 
                !resource.getEmail().equals(existing.getEmail()) &&
                resourceRepository.existsByEmail(resource.getEmail())) {
                throw new ProjectExceptionHandler(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_EMAIL",
                        "Email already exists"
                );
            }

            resourceRepository.save(resource);

            // Trigger async availability ledger recalculation for current month only
            resourceEventService.triggerLedgerCalculationAfterUpdate(resource.getResourceId());
            
            return ResponseEntity.ok(
                    new ApiResponse(true, "Resource updated successfully", existing.getResourceId())
            );

        } catch (ProjectExceptionHandler e) {
            ApiResponse response = new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, e.getStatus());

        } catch (Exception e) {
            ApiResponse response = new ApiResponse(
                    false,
                    "Failed to update resource: " + e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteResource(Long resourceId) {
        try {
            if (resourceId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "RESOURCE_ID_REQUIRED",
                        "Resource ID is required for deletion"
                );
            }

            Resource existing = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "RESOURCE_NOT_FOUND",
                            "Resource not found"
                    ));

            // Delete all ledger entries for this resource
            ledgerRepository.deleteByResourceId(resourceId);

            // Delete the resource
            resourceRepository.delete(existing);

            // Trigger async cleanup
            resourceEventService.triggerLedgerCleanupAfterDelete(resourceId);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Resource deleted successfully", resourceId)
            );

        } catch (ProjectExceptionHandler e) {
            ApiResponse response = new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, e.getStatus());

        } catch (Exception e) {
            ApiResponse response = new ApiResponse(
                    false,
                    "Failed to delete resource: " + e.getMessage(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getAllResources() {
        List<String> uniqueLocations = resourceRepository.findDistinctLocations();
        List<String> uniqueDesignations = resourceRepository.findDistinctDesignations();
        List<WorkforceCategory> workforceCategories = List.of(WorkforceCategory.values());
        Long maxExperience = resourceRepository.findMaxExperience();
        List<String> projectNames = projectRepo.findProjectNamesExceptStatus(ProjectStatus.COMPLETED);
        ResourceFiltersDTO dto = new ResourceFiltersDTO(uniqueLocations, workforceCategories, uniqueDesignations, maxExperience, projectNames);
        return ResponseEntity.ok(new ApiResponse<>(true, "Resource Filters retrieved successfully", dto));
    }

    @Override
    public ResponseEntity<?> getResources() {
        List<ResourceNameDTO> resources = resourceRepository.findAll().stream()
                .map(resource -> new ResourceNameDTO(resource.getFullName(), resource.getResourceId(), resource.getDesignation()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Resources retrieved successfully", resources));
    }
}
