package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ResourceEnablementAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;

public interface ResourceEnablementAssignmentService {
    ApiResponse<String> requestEnablement(ResourceEnablementAssignment assignment);

    ApiResponse<String> updateStatus(
            Long assignmentId,
            EnablementAssignmentStatus status,
            String remarks
    );

    ApiResponse<?> getAssignmentsByResource(Long resourceId);
}
