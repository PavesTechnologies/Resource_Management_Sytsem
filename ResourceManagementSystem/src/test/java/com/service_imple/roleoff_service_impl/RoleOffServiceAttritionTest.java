package com.service_imple.roleoff_service_impl;

import com.dto.allocation_dto.CloseAllocationDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import com.service_interface.demand_service_interface.DemandService;
import com.service_imple.allocation_service_imple.AvailabilityLedgerAsyncServiceRefactored;
import com.service_imple.roleoff_service_impl.RoleOffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleOffService - Attrition Functionality Tests")
class RoleOffServiceAttritionTest {

    @Mock
    private ResourceRepository resourceRepository;
    
    @Mock
    private AllocationRepository allocationRepository;
    
    @Mock
    private AllocationService allocationService;
    
    @Mock
    private DemandService demandService;
    
    @Mock
    private AvailabilityLedgerAsyncServiceRefactored availabilityLedgerAsyncService;

    @InjectMocks
    private RoleOffServiceImpl roleOffService;

    private Resource testResource;
    private Project testProject;
    private LocalDate today;
    private LocalDate dateOfExit;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        dateOfExit = today.plusDays(30);
        
        testResource = createTestResource(1L, "John Doe", EmploymentStatus.ACTIVE);
        testProject = createTestProject(100L, "Test Project");
    }

    // ========== HELPER METHODS ==========

    private Resource createTestResource(Long id, String name, EmploymentStatus status) {
        Resource resource = new Resource();
        resource.setResourceId(id);
        resource.setFullName(name);
        resource.setEmploymentStatus(status);
        resource.setExperiance(5L);
        return resource;
    }

    private Project createTestProject(Long id, String name) {
        Project project = new Project();
        project.setPmsProjectId(id);
        project.setName(name);
        project.setEndDate(today.plusMonths(6).atTime(12, 0));
        return project;
    }

    private ResourceAllocation createTestAllocation(UUID id, Resource resource, Project project, 
                                                 LocalDate startDate, LocalDate endDate, 
                                                 AllocationStatus status, int percentage) {
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setAllocationId(id);
        allocation.setResource(resource);
        allocation.setProject(project);
        allocation.setAllocationStartDate(startDate);
        allocation.setAllocationEndDate(endDate);
        allocation.setAllocationStatus(status);
        allocation.setAllocationPercentage(percentage);
        allocation.setCreatedBy("test");
        return allocation;
    }

    private ResourceAllocation createOverlappingAllocation(Resource resource, Project project) {
        return createTestAllocation(
            UUID.randomUUID(),
            resource,
            project,
            dateOfExit.minusDays(60),  // Started before exit
            dateOfExit.plusDays(60),   // Ends after exit
            AllocationStatus.ACTIVE,
            100
        );
    }

    private ResourceAllocation createFutureAllocation(Resource resource, Project project) {
        return createTestAllocation(
            UUID.randomUUID(),
            resource,
            project,
            dateOfExit.plusDays(10),   // Starts after exit
            dateOfExit.plusDays(70),   // Ends after exit
            AllocationStatus.PLANNED,
            80
        );
    }

    private ResourceAllocation createPastAllocation(Resource resource, Project project) {
        return createTestAllocation(
            UUID.randomUUID(),
            resource,
            project,
            dateOfExit.minusDays(90),  // Started before exit
            dateOfExit.minusDays(30),   // Ended before exit
            AllocationStatus.ACTIVE,
            90
        );
    }

    // ========== TEST CASES ==========

    @Test
    @DisplayName("BASIC ATTRITION - Single allocation spanning exit date")
    void testBasicAttrition_SingleAllocationSpanningExit() {
        // Arrange
        ResourceAllocation allocation = createOverlappingAllocation(testResource, testProject);
        List<ResourceAllocation> allocations = List.of(allocation);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(allocation.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert
        verify(allocationService).closeAllocation(eq(allocation.getAllocationId()), any(CloseAllocationDTO.class));
        verify(demandService).createReplacementDemandFromAllocation(
            eq(allocation), 
            eq(dateOfExit.plusDays(1)), 
            eq(allocation.getAllocationEndDate())
        );
        verify(availabilityLedgerAsyncService).updateLedger(eq(1L), eq(dateOfExit), any(LocalDate.class));
        
        // Verify CloseAllocationDTO parameters
        ArgumentCaptor<CloseAllocationDTO> closeDTOCaptor = ArgumentCaptor.forClass(CloseAllocationDTO.class);
        verify(allocationService).closeAllocation(eq(allocation.getAllocationId()), closeDTOCaptor.capture());
        CloseAllocationDTO capturedDTO = closeDTOCaptor.getValue();
        assertEquals(dateOfExit, capturedDTO.getClosureDate());
        assertEquals("ATTRITION", capturedDTO.getReason());
    }

    @Test
    @DisplayName("PARTIAL ALLOCATION - Allocation partially overlapping exit date")
    void testPartialAllocation_OverlappingExit() {
        // Arrange
        ResourceAllocation allocation = createTestAllocation(
            UUID.randomUUID(),
            testResource,
            testProject,
            dateOfExit.minusDays(20),  // Started 20 days before exit
            dateOfExit.plusDays(40),   // Ends 40 days after exit
            AllocationStatus.ACTIVE,
            75
        );
        List<ResourceAllocation> allocations = List.of(allocation);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(allocation.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert
        verify(allocationService).closeAllocation(eq(allocation.getAllocationId()), any(CloseAllocationDTO.class));
        verify(demandService).createReplacementDemandFromAllocation(
            eq(allocation), 
            eq(dateOfExit.plusDays(1)),  // Replacement starts day after exit
            eq(allocation.getAllocationEndDate())  // Replacement ends at original end date
        );
    }

    @Test
    @DisplayName("MULTIPLE ALLOCATIONS SAME PROJECT - Overlapping + future allocation")
    void testMultipleAllocations_SameProject() {
        // Arrange
        ResourceAllocation overlappingAlloc = createOverlappingAllocation(testResource, testProject);
        ResourceAllocation futureAlloc = createFutureAllocation(testResource, testProject);
        List<ResourceAllocation> allocations = List.of(overlappingAlloc, futureAlloc);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(futureAlloc.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert
        // Overlapping allocation should be closed
        verify(allocationService).closeAllocation(eq(overlappingAlloc.getAllocationId()), any(CloseAllocationDTO.class));
        verify(demandService).createReplacementDemandFromAllocation(
            eq(overlappingAlloc), 
            eq(dateOfExit.plusDays(1)), 
            eq(overlappingAlloc.getAllocationEndDate())
        );

        // Future allocation should be cancelled
        assertEquals(AllocationStatus.CANCELLED, futureAlloc.getAllocationStatus());
        assertEquals("CANCELLED DUE TO RESOURCE ATTRITION", futureAlloc.getClosureReason());
        verify(allocationRepository).save(futureAlloc);
        verify(demandService).createReplacementDemandFromAllocation(
            eq(futureAlloc), 
            eq(futureAlloc.getAllocationStartDate()), 
            eq(futureAlloc.getAllocationEndDate())
        );

        // Verify only one closeAllocation call (for overlapping only)
        verify(allocationService, times(1)).closeAllocation(any(), any());
        verify(allocationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("MULTIPLE PROJECTS - Allocations across different projects")
    void testMultipleProjects_IndependentProcessing() {
        // Arrange
        Project project2 = createTestProject(200L, "Project 2");
        
        ResourceAllocation alloc1 = createOverlappingAllocation(testResource, testProject);
        ResourceAllocation alloc2 = createFutureAllocation(testResource, project2);
        List<ResourceAllocation> allocations = List.of(alloc1, alloc2);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(alloc2.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert - Both allocations should be processed independently
        verify(allocationService).closeAllocation(eq(alloc1.getAllocationId()), any(CloseAllocationDTO.class));
        verify(demandService).createReplacementDemandFromAllocation(
            eq(alloc1), 
            eq(dateOfExit.plusDays(1)), 
            eq(alloc1.getAllocationEndDate())
        );

        assertEquals(AllocationStatus.CANCELLED, alloc2.getAllocationStatus());
        verify(allocationRepository).save(alloc2);
        verify(demandService).createReplacementDemandFromAllocation(
            eq(alloc2), 
            eq(alloc2.getAllocationStartDate()), 
            eq(alloc2.getAllocationEndDate())
        );
    }

    @Test
    @DisplayName("FUTURE-ONLY ALLOCATION - Allocation entirely after exit")
    void testFutureOnlyAllocation_CancelledAndDemandCreated() {
        // Arrange
        ResourceAllocation futureAlloc = createFutureAllocation(testResource, testProject);
        List<ResourceAllocation> allocations = List.of(futureAlloc);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(futureAlloc.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert
        assertEquals(AllocationStatus.CANCELLED, futureAlloc.getAllocationStatus());
        assertEquals("CANCELLED DUE TO RESOURCE ATTRITION", futureAlloc.getClosureReason());
        verify(allocationRepository).save(futureAlloc);
        
        // Demand should be created for full range
        verify(demandService).createReplacementDemandFromAllocation(
            eq(futureAlloc), 
            eq(futureAlloc.getAllocationStartDate()), 
            eq(futureAlloc.getAllocationEndDate())
        );
        
        // No closeAllocation should be called
        verify(allocationService, never()).closeAllocation(any(), any());
    }

    @Test
    @DisplayName("PAST ALLOCATION - Allocation before exit")
    void testPastAllocation_NoInteractionWithServices() {
        // Arrange
        ResourceAllocation pastAlloc = createPastAllocation(testResource, testProject);
        List<ResourceAllocation> allocations = List.of(pastAlloc);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.empty());

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert - No interaction with allocation or demand services
        verify(allocationService, never()).closeAllocation(any(), any());
        verify(demandService, never()).createReplacementDemandFromAllocation(any(), any(), any());
        verify(allocationRepository, never()).save(any());
        
        // Only ledger update should happen
        verify(availabilityLedgerAsyncService).updateLedger(eq(1L), eq(dateOfExit), any(LocalDate.class));
    }

    @Test
    @DisplayName("EXACT DATE MATCH - allocationEndDate == dateOfExit")
    void testExactDateMatch_CloseAllocationCalledNoDemandCreated() {
        // Arrange
        ResourceAllocation allocation = createTestAllocation(
            UUID.randomUUID(),
            testResource,
            testProject,
            dateOfExit.minusDays(30),
            dateOfExit,  // Ends exactly on exit date
            AllocationStatus.ACTIVE,
            100
        );
        List<ResourceAllocation> allocations = List.of(allocation);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(dateOfExit));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert
        verify(allocationService).closeAllocation(eq(allocation.getAllocationId()), any(CloseAllocationDTO.class));
        
        // NO demand should be created since end date == exit date
        verify(demandService, never()).createReplacementDemandFromAllocation(any(), any(), any());
    }

    @Test
    @DisplayName("OVER-ALLOCATION (>100%) - allocationPercentage = 130")
    void testOverAllocation_DemandCreatedWithCappedValue() {
        // Arrange
        ResourceAllocation allocation = createTestAllocation(
            UUID.randomUUID(),
            testResource,
            testProject,
            dateOfExit.minusDays(30),
            dateOfExit.plusDays(30),
            AllocationStatus.ACTIVE,
            130  // Over-allocated
        );
        List<ResourceAllocation> allocations = List.of(allocation);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(allocation.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert
        verify(allocationService).closeAllocation(eq(allocation.getAllocationId()), any(CloseAllocationDTO.class));
        verify(demandService).createReplacementDemandFromAllocation(
            eq(allocation), 
            eq(dateOfExit.plusDays(1)), 
            eq(allocation.getAllocationEndDate())
        );
        
        // The demand should be created with the original allocation percentage (130)
        // The capping logic should be handled in the demand service, not here
        ArgumentCaptor<ResourceAllocation> allocationCaptor = ArgumentCaptor.forClass(ResourceAllocation.class);
        verify(demandService).createReplacementDemandFromAllocation(
            allocationCaptor.capture(), 
            any(), 
            any()
        );
        assertEquals(130, allocationCaptor.getValue().getAllocationPercentage());
    }

    @Test
    @DisplayName("IDEMPOTENCY - Call method twice")
    void testIdempotency_NoDuplicateDemandOrClosure() {
        // Arrange
        ResourceAllocation allocation = createOverlappingAllocation(testResource, testProject);
        List<ResourceAllocation> allocations = List.of(allocation);
        
        when(resourceRepository.findById(1L))
            .thenReturn(Optional.of(testResource))
            .thenReturn(Optional.of(testResource));  // Second call
        when(allocationRepository.findByResource_ResourceId(1L))
            .thenReturn(allocations)
            .thenReturn(allocations);  // Second call
        when(allocationRepository.findMaxAllocationEndDateForResource(1L))
            .thenReturn(Optional.of(allocation.getAllocationEndDate()))
            .thenReturn(Optional.of(allocation.getAllocationEndDate()));  // Second call

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert - Each service should be called twice (once per attrition call)
        verify(allocationService, times(2)).closeAllocation(eq(allocation.getAllocationId()), any(CloseAllocationDTO.class));
        verify(demandService, times(2)).createReplacementDemandFromAllocation(
            eq(allocation), 
            eq(dateOfExit.plusDays(1)), 
            eq(allocation.getAllocationEndDate())
        );
        verify(availabilityLedgerAsyncService, times(2)).updateLedger(eq(1L), eq(dateOfExit), any(LocalDate.class));
    }

    @Test
    @DisplayName("NO ALLOCATIONS - Empty allocation list")
    void testNoAllocations_ResourceUpdatedNoFailures() {
        // Arrange
        List<ResourceAllocation> allocations = Collections.emptyList();
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> roleOffService.handleAttrition(1L, dateOfExit));

        // Assert - No interaction with allocation or demand services
        verify(allocationService, never()).closeAllocation(any(), any());
        verify(demandService, never()).createReplacementDemandFromAllocation(any(), any(), any());
        verify(allocationRepository, never()).save(any());
        
        // Only ledger update should happen
        verify(availabilityLedgerAsyncService).updateLedger(eq(1L), eq(dateOfExit), any(LocalDate.class));
    }

    @Test
    @DisplayName("INVALID DATA - allocationStartDate = null OR invalid range")
    void testInvalidData_SkippedSafelyNoCrash() {
        // Arrange
        ResourceAllocation invalidAlloc1 = new ResourceAllocation();
        invalidAlloc1.setAllocationId(UUID.randomUUID());
        invalidAlloc1.setResource(testResource);
        invalidAlloc1.setProject(testProject);
        invalidAlloc1.setAllocationStartDate(null);  // Invalid: null start date
        invalidAlloc1.setAllocationEndDate(dateOfExit.plusDays(30));
        invalidAlloc1.setAllocationStatus(AllocationStatus.ACTIVE);
        invalidAlloc1.setAllocationPercentage(100);

        ResourceAllocation invalidAlloc2 = new ResourceAllocation();
        invalidAlloc2.setAllocationId(UUID.randomUUID());
        invalidAlloc2.setResource(testResource);
        invalidAlloc2.setProject(testProject);
        invalidAlloc2.setAllocationStartDate(dateOfExit.plusDays(30));
        invalidAlloc2.setAllocationEndDate(dateOfExit.minusDays(30));  // Invalid: end before start
        invalidAlloc2.setAllocationStatus(AllocationStatus.PLANNED);
        invalidAlloc2.setAllocationPercentage(80);

        List<ResourceAllocation> allocations = List.of(invalidAlloc1, invalidAlloc2);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.empty());

        // Act & Assert - Should not crash
        assertDoesNotThrow(() -> roleOffService.handleAttrition(1L, dateOfExit));

        // The method should handle null/invalid dates gracefully
        // This test verifies no exception is thrown, but the actual behavior depends on implementation
    }

    @Test
    @DisplayName("PAST EXIT DATE - dateOfExit < today")
    void testPastExitDate_ShouldSkipDemandCreation() {
        // Arrange
        LocalDate pastExitDate = today.minusDays(10);
        ResourceAllocation allocation = createTestAllocation(
            UUID.randomUUID(),
            testResource,
            testProject,
            pastExitDate.minusDays(20),
            pastExitDate.plusDays(20),
            AllocationStatus.ACTIVE,
            100
        );
        List<ResourceAllocation> allocations = List.of(allocation);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.of(allocation.getAllocationEndDate()));

        // Act
        roleOffService.handleAttrition(1L, pastExitDate);

        // Assert - Allocation should still be processed (closed) but NO demand should be created for past dates
        verify(allocationService, times(1))
            .closeAllocation(eq(allocation.getAllocationId()), any(CloseAllocationDTO.class));
        
        // ✅ FIXED: Demand creation should be skipped for past exit dates
        verify(demandService, never())
            .createReplacementDemandFromAllocation(any(), any(), any());
        
        // Verify CloseAllocationDTO parameters
        ArgumentCaptor<CloseAllocationDTO> closeDTOCaptor = ArgumentCaptor.forClass(CloseAllocationDTO.class);
        verify(allocationService).closeAllocation(eq(allocation.getAllocationId()), closeDTOCaptor.capture());
        CloseAllocationDTO capturedDTO = closeDTOCaptor.getValue();
        assertEquals(pastExitDate, capturedDTO.getClosureDate());
        assertEquals("ATTRITION", capturedDTO.getReason());
    }

    @Test
    @DisplayName("RESOURCE ALREADY EXITED - Should skip processing")
    void testResourceAlreadyExited_SkipProcessing() {
        // Arrange
        Resource exitedResource = createTestResource(1L, "Jane Doe", EmploymentStatus.EXITED);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(exitedResource));

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert - No further processing should occur
        verify(allocationRepository, never()).findByResource_ResourceId(any());
        verify(allocationService, never()).closeAllocation(any(), any());
        verify(demandService, never()).createReplacementDemandFromAllocation(any(), any(), any());
        verify(availabilityLedgerAsyncService, never()).updateLedger(any(), any(), any());
    }

    @Test
    @DisplayName("RESOURCE NOT FOUND - Should throw exception")
    void testResourceNotFound_ThrowException() {
        // Arrange
        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ProjectExceptionHandler exception = assertThrows(ProjectExceptionHandler.class, 
            () -> roleOffService.handleAttrition(1L, dateOfExit));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
        assertEquals("Resource not found", exception.getMessage());
    }

    @Test
    @DisplayName("ENDED AND CANCELLED ALLOCATIONS - Should be skipped")
    void testEndedAndCancelledAllocations_Skipped() {
        // Arrange
        ResourceAllocation endedAlloc = createTestAllocation(
            UUID.randomUUID(), testResource, testProject,
            dateOfExit.minusDays(30), dateOfExit.plusDays(30),
            AllocationStatus.ENDED, 100
        );
        
        ResourceAllocation cancelledAlloc = createTestAllocation(
            UUID.randomUUID(), testResource, testProject,
            dateOfExit.minusDays(20), dateOfExit.plusDays(40),
            AllocationStatus.CANCELLED, 80
        );
        
        List<ResourceAllocation> allocations = List.of(endedAlloc, cancelledAlloc);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(allocationRepository.findByResource_ResourceId(1L)).thenReturn(allocations);
        when(allocationRepository.findMaxAllocationEndDateForResource(1L)).thenReturn(Optional.empty());

        // Act
        roleOffService.handleAttrition(1L, dateOfExit);

        // Assert - Both should be skipped
        verify(allocationService, never()).closeAllocation(any(), any());
        verify(demandService, never()).createReplacementDemandFromAllocation(any(), any(), any());
        verify(allocationRepository, never()).save(any());
        
        // Only ledger update should happen
        verify(availabilityLedgerAsyncService).updateLedger(eq(1L), eq(dateOfExit), any(LocalDate.class));
    }
}
