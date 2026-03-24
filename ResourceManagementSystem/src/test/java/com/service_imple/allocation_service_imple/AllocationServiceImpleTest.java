package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.CloseAllocationDTO;
import com.dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationServiceImpleTest {

    @Mock
    private AllocationRepository allocationRepository;
    
    @Mock
    private ResourceRepository resourceRepository;
    
    @Mock
    private ResourceAvailabilityLedgerRepository ledgerRepository;
    
    @Mock
    private AvailabilityCalculationService availabilityCalculationService;
    
    @Mock
    private AvailabilityLedgerAsyncService ledgerAsyncService;
    
    @InjectMocks
    private AllocationServiceImple allocationService;

    @Test
    void testCloseAllocation_ImmediateAvailabilityRecalculation() {
        // Arrange
        UUID allocationId = UUID.randomUUID();
        Long resourceId = 123L;
        LocalDate closureDate = LocalDate.of(2026, 3, 17);
        
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setAllocationId(allocationId);
        allocation.setResource(resource);
        allocation.setAllocationStatus(AllocationStatus.ACTIVE);
        allocation.setAllocationStartDate(LocalDate.of(2026, 1, 1));
        allocation.setAllocationEndDate(LocalDate.of(2026, 6, 30));
        
        CloseAllocationDTO request = new CloseAllocationDTO();
        request.setClosureDate(closureDate);
        
        when(allocationRepository.findById(allocationId)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(any(ResourceAllocation.class))).thenReturn(allocation);
        
        // Act
        ResponseEntity<ApiResponse<?>> response = allocationService.closeAllocation(allocationId, request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        assertEquals("Allocation closed successfully with immediate availability update", 
                    response.getBody().getMessage());
        
        // Verify availability recalculation was triggered
        verify(availabilityCalculationService, atLeastOnce()).recalculateForResource(eq(resourceId), any(YearMonth.class));
        verify(ledgerAsyncService).triggerLedgerUpdateForResource(resourceId);
        
        // Verify allocation status was updated
        assertEquals(AllocationStatus.ENDED, allocation.getAllocationStatus());
        assertEquals(closureDate, allocation.getAllocationEndDate());
    }

    @Test
    void testCloseAllocation_AllocationNotFound() {
        // Arrange
        UUID allocationId = UUID.randomUUID();
        CloseAllocationDTO request = new CloseAllocationDTO();
        request.setClosureDate(LocalDate.now());
        
        when(allocationRepository.findById(allocationId)).thenReturn(Optional.empty());
        
        // Act
        ResponseEntity<ApiResponse<?>> response = allocationService.closeAllocation(allocationId, request);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify availability recalculation was NOT triggered
        verify(availabilityCalculationService, never()).recalculateForResource(anyLong(), any(YearMonth.class));
        verify(ledgerAsyncService, never()).triggerLedgerUpdateForResource(anyLong());
    }

    @Test
    void testCloseAllocation_AlreadyEnded() {
        // Arrange
        UUID allocationId = UUID.randomUUID();
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setAllocationId(allocationId);
        allocation.setAllocationStatus(AllocationStatus.ENDED);
        
        CloseAllocationDTO request = new CloseAllocationDTO();
        request.setClosureDate(LocalDate.now());
        
        when(allocationRepository.findById(allocationId)).thenReturn(Optional.of(allocation));
        
        // Act
        ResponseEntity<ApiResponse<?>> response = allocationService.closeAllocation(allocationId, request);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().getSuccess());
        
        // Verify availability recalculation was NOT triggered
        verify(availabilityCalculationService, never()).recalculateForResource(anyLong(), any(YearMonth.class));
        verify(ledgerAsyncService, never()).triggerLedgerUpdateForResource(anyLong());
    }

    @Test
    void testGetAffectedPeriods() throws Exception {
        // Use reflection to test private method
        var method = AllocationServiceImple.class.getDeclaredMethod("getAffectedPeriods", LocalDate.class);
        method.setAccessible(true);
        
        LocalDate allocationEndDate = LocalDate.of(2026, 3, 17);
        
        @SuppressWarnings("unchecked")
        java.util.List<YearMonth> periods = (java.util.List<YearMonth>) method.invoke(allocationService, allocationEndDate);
        
        // Should include current month, allocation month, and next 2 months
        assertTrue(periods.size() >= 3);
        assertTrue(periods.contains(YearMonth.of(2026, 3))); // Current/allocation month
        assertTrue(periods.contains(YearMonth.of(2026, 4))); // Next month
        assertTrue(periods.contains(YearMonth.of(2026, 5))); // Following month
    }
}
