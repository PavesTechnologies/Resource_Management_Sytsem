package com.service_interface.resource_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.entity.resource_entities.Resource;
import org.springframework.http.ResponseEntity;

public interface ResourceService {
    ResponseEntity<ApiResponse<?>> createResource(Resource resource);
    ResponseEntity<ApiResponse<?>> getResourceById(String resourceId);
    ResponseEntity<ApiResponse<?>> getResourceByEmployeeCode(String employeeCode);
    ResponseEntity<ApiResponse<?>> updateResource(Resource resource);
    ResponseEntity<ApiResponse<?>> deleteResource(String resourceId);
    ResponseEntity<?> getAllResources();
    ResponseEntity<?> getResources();
}
