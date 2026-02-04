package com.service_imple.client_service_impl;


import com.dto.client_dto.ApiResponse;
import com.entity.client_entities.ResourceEnablementAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ResourceEnablementAssignmentRepository;
import com.service_interface.client_service_interface.ResourceEnablementAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

            return new ApiResponse<>(
                    true,
                    "Enablement requested successfully.",
                    null
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Enablement request failed: " + e.getMessage(),
                    null
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

            return new ApiResponse<>(
                    true,
                    "Enablement status updated successfully.",
                    null
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Status update failed: " + e.getMessage(),
                    null
            );
        }
    }

    @Override
    public ApiResponse<?> getAssignmentsByResource(Long resourceId) {
        return new ApiResponse<>(
                true,
                "Enablement assignments fetched successfully.",
                repository.findByResourceId(resourceId)
        );
    }

}
