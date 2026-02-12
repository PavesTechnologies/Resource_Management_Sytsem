package com.service_imple.client_service_impl;


import com.dto.ApiResponse;
import com.entity.client_entities.ResourceEnablementAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ResourceEnablementAssignmentRepository;
import com.service_interface.client_service_interface.ResourceEnablementAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceEnablementAssignmentServiceImpl implements ResourceEnablementAssignmentService {
    private final ResourceEnablementAssignmentRepository repository;
    private final ClientAssetRepository assetRepository;

    @Override
    public ApiResponse<String> requestEnablement(ResourceEnablementAssignment assignment) {
        try {
            assetRepository.findById(
                    assignment.getClientAsset().getAssetId()
            ).orElseThrow(() -> new RuntimeException("Enablement not found"));

            assignment.setStatus(EnablementAssignmentStatus.REQUESTED);
            assignment.setRequestedAt(LocalDateTime.now());

            repository.save(assignment);

            return ApiResponse.<String>success(
                    "Enablement requested successfully.",
                    null
            );
        } catch (Exception e) {
            return ApiResponse.<String>error(
                    "Enablement request failed: " + e.getMessage()
            );
        }
    }

    @Override
    public ApiResponse<String> updateStatus(
            UUID assignmentId,
            EnablementAssignmentStatus status,
            String remarks) {
        try {
            ResourceEnablementAssignment assignment =
                    repository.findById(assignmentId)
                            .orElseThrow(() -> new RuntimeException("Assignment not found"));

            assignment.setStatus(status);
            assignment.setRemarks(remarks);
            assignment.setActionedAt(LocalDateTime.now());

            repository.save(assignment);

            return ApiResponse.<String>success(
                    "Enablement status updated successfully.",
                    null
            );
        } catch (Exception e) {
            return ApiResponse.<String>error(
                    "Status update failed: " + e.getMessage()
            );
        }
    }

    @Override
    public ApiResponse<?> getAssignmentsByResource(Long resourceId) {
        return ApiResponse.<List<ResourceEnablementAssignment>>success(
                "Enablement assignments fetched successfully.",
                repository.findByResourceId(resourceId)
        );
    }

}
