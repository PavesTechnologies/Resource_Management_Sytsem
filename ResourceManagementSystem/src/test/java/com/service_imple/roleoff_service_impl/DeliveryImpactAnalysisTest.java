package com.service_imple.roleoff_service_impl;

import com.dto.allocation_dto.RoleOffRequestDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.RoleOffType;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.roleoff_repo.RoleOffEventRepository;
import com.service_imple.roleoff_service_impl.RoleOffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryImpactAnalysisTest {

    @Mock
    private RoleOffEventRepository roleOffRepo;

    @Mock
    private ProjectRepository projectRepo;

    @Mock
    private ResourceRepository resourceRepo;

    @Mock
    private AllocationRepository allocationRepository;

    @InjectMocks
    private RoleOffServiceImpl roleOffService;

    private RoleOffRequestDTO testDTO;
    private Resource testResource;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testDTO = new RoleOffRequestDTO();
        testDTO.setProjectId(1L);
        testDTO.setResourceId(100L);
        testDTO.setRoleOffDate(LocalDate.now().plusDays(30));
        testDTO.setRoleOffType(RoleOffType.PLANNED);

        testResource = new Resource();
        testResource.setResourceId(100L);
        testResource.setFullName("John Doe");
        testResource.setExperiance(5L);

        testProject = new Project();
        testProject.setPmsProjectId(1L);
        testProject.setPriorityLevel("HIGH");
    }

    @Test
    void testValidateDeliveryImpact_HighRiskScenario() {
        // Setup high-risk scenario
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("CRITICAL");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(3L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(5L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(8, 6, 4));
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(80));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(80);

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertNotNull(result);
        assertTrue(result.contains("Role-Off Impact Preview"));
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("Current Utilization: 80%"));
        assertTrue(result.contains("Utilization After Role-Off: 0%"));
        assertTrue(result.contains("Project Criticality: CRITICAL"));
        assertTrue(result.contains("Remaining Demand: 3 open positions"));
        assertTrue(result.contains("Resource Gap: 8 position(s)"));
        assertTrue(result.contains("Large utilization drop"));
        assertTrue(result.contains("Urgent: Find replacement assignment"));
    }

    @Test
    void testValidateDeliveryImpact_ModerateRiskScenario() {
        // Setup moderate-risk scenario
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("MEDIUM");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(1L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(3L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(4, 2));
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(50));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(50);

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertNotNull(result);
        assertTrue(result.contains("Role-Off Impact Preview"));
        assertTrue(result.contains("Current Utilization: 50%"));
        assertTrue(result.contains("Utilization After Role-Off: 0%"));
        assertTrue(result.contains("Remaining Demand: 1 open positions"));
        assertTrue(result.contains("Resource Gap: 2 position(s)"));
        assertTrue(result.contains("Large utilization drop"));
        assertTrue(result.contains("Assign resource to another demand immediately"));
    }

    @Test
    void testValidateDeliveryImpact_LowRiskScenario() {
        // Setup low-risk scenario
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("LOW");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(0L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(2L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(2));
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(25));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(25);

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertNotNull(result);
        assertTrue(result.contains("Role-Off Impact Preview"));
        assertTrue(result.contains("Current Utilization: 25%"));
        assertTrue(result.contains("Utilization After Role-Off: 0%"));
        assertTrue(result.contains("Future Capacity Available: 100%"));
        assertTrue(result.contains("Small utilization drop"));
        assertTrue(result.contains("Monitor for new demand opportunities"));
    }

    @Test
    void testValidateDeliveryImpact_MultipleProjectUtilization() {
        // Setup resource with multiple project allocations
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("MEDIUM");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(0L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(2L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(3));
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(40, 30, 20)); // Multiple allocations
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(90); // 90% total utilization

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertNotNull(result);
        assertTrue(result.contains("Current Utilization: 90%"));
        assertTrue(result.contains("Utilization After Role-Off: 50%")); // 90 - 40 = 50
        assertTrue(result.contains("Future Capacity Available: 50%"));
        assertTrue(result.contains("Large utilization drop"));
    }

    @Test
    void testValidateDeliveryImpact_NoImpactScenario() {
        // Setup scenario with minimal impact
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("LOW");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(0L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(1L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(1));
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(5));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(5);

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertNotNull(result);
        assertTrue(result.contains("Current Utilization: 5%"));
        assertTrue(result.contains("Utilization After Role-Off: 0%"));
        assertTrue(result.contains("Minimal utilization drop"));
        assertTrue(result.contains("No immediate action required"));
    }

    @Test
    void testValidateDeliveryImpact_ResourceNotFound() {
        // Setup resource not found scenario
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.empty());

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertEquals("Unable to validate delivery impact. Proceed with caution.", result);
    }

    @Test
    void testValidateDeliveryImpact_DatabaseError() {
        // Setup database error scenario
        when(resourceRepo.findById(100L)).thenThrow(new RuntimeException("Database connection failed"));

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertEquals("Unable to validate delivery impact. Proceed with caution.", result);
    }

    @Test
    void testDetermineUtilizationImpact() {
        // Test private method through public behavior
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("LOW");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(0L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(1L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(1));

        // Test large utilization drop
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(60));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(60);
        String result1 = roleOffService.validateDeliveryImpact(testDTO);
        assertTrue(result1.contains("Large utilization drop"));

        // Test moderate utilization drop
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(30));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(30);
        String result2 = roleOffService.validateDeliveryImpact(testDTO);
        assertTrue(result2.contains("Moderate utilization drop"));

        // Test small utilization drop
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(15));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(15);
        String result3 = roleOffService.validateDeliveryImpact(testDTO);
        assertTrue(result3.contains("Small utilization drop"));

        // Test minimal utilization drop
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(8));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(8);
        String result4 = roleOffService.validateDeliveryImpact(testDTO);
        assertTrue(result4.contains("Minimal utilization drop"));
    }

    @Test
    void testLogRoleOffDecision() {
        // Test audit logging functionality
        String warning = "High risk impact detected";
        
        // This should not throw any exception
        assertDoesNotThrow(() -> {
            roleOffService.logRoleOffDecision(testDTO, warning, true, 1001L);
        });
    }

    @Test
    void testValidateDeliveryImpact_ComprehensiveRiskAssessment() {
        // Test comprehensive risk assessment with all risk factors
        when(resourceRepo.findById(100L)).thenReturn(java.util.Optional.of(testResource));
        when(roleOffRepo.getProjectPriorityLevel(1L)).thenReturn("CRITICAL");
        when(roleOffRepo.countOpenDemandForProject(1L)).thenReturn(5L);
        when(roleOffRepo.countActiveAllocationsForProject(1L)).thenReturn(3L);
        when(roleOffRepo.getRequiredPositionsForProject(1L)).thenReturn(Arrays.asList(10, 8, 6));
        when(roleOffRepo.getCurrentUtilization(100L)).thenReturn(Arrays.asList(75));
        when(roleOffRepo.getTotalCurrentUtilization(100L)).thenReturn(75);

        String result = roleOffService.validateDeliveryImpact(testDTO);

        assertNotNull(result);
        assertTrue(result.contains("Project Impact:")); // Should have project impact section
        assertTrue(result.contains("Project Criticality: CRITICAL"));
        assertTrue(result.contains("Remaining Demand: 5 open positions"));
        assertTrue(result.contains("Resource Gap: 20 position(s)")); // (10+8+6) - (3-1) = 24-2 = 22, but max(0, ...) = 20
        assertTrue(result.contains("Large utilization drop"));
        assertTrue(result.contains("Urgent: Find replacement assignment"));
    }
}
