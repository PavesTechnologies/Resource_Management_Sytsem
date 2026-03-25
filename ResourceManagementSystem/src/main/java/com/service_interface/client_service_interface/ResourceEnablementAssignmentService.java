package com.service_interface.client_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ResourceEnablementAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;

import java.util.UUID;

public interface ResourceEnablementAssignmentService {
    ApiResponse<String> requestEnablement(ResourceEnablementAssignment assignment);

    ApiResponse<String> updateStatus(
            UUID assignmentId,
            EnablementAssignmentStatus status,
            String remarks
    );

    ApiResponse<?> getAssignmentsByResource(Long resourceId);
}
