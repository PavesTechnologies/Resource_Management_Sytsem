package com.service_interface.allocation_service_interface;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface AllocationService {
    
    ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest);
    
    ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId);
    
    ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest);
    
    ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByResource(Long resourceId);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId);
}
