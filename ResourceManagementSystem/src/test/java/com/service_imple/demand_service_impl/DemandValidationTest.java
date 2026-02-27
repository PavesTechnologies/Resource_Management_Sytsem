package com.service_imple.demand_service_impl;

import com.dto.demand_dto.CreateDemandDTO;
import com.dto.demand_dto.DemandConflictValidationDTO;
import com.entity.project_entities.Project;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandType;
import com.repo.demand_repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DemandValidationTest {

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DeliveryRoleExpectationRepository roleRepository;

    @InjectMocks
    private DemandServiceImpl demandService;

    private CreateDemandDTO testDTO;
    private Project testProject;
    private DeliveryRoleExpectation testRole;

    @BeforeEach
    void setUp() {
        testDTO = new CreateDemandDTO();
        testDTO.setProjectId(1L);
        testDTO.setDeliveryRole(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")); // Valid UUID
        testDTO.setDemandName("Test Demand");
        testDTO.setDemandType(DemandType.NET_NEW);
        testDTO.setDemandStartDate(LocalDate.of(2026, 3, 1));
        testDTO.setDemandEndDate(LocalDate.of(2026, 3, 31));
        testDTO.setAllocationPercentage(50);
        testDTO.setDemandPriority(PriorityLevel.HIGH);
        testDTO.setResourcesRequired(1);
        testDTO.setDemandJustification("This is a test demand justification for testing purposes");

        testProject = new Project();
        testProject.setPmsProjectId(1L);
        testProject.setName("Test Project");

        testRole = new DeliveryRoleExpectation();
        testRole.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        testRole.setRoleName("Developer");
    }

    @Test
    void testValidateDemandConflicts_NoConflicts() {
        // Setup
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(roleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))).thenReturn(Optional.of(testRole));
        when(demandRepository.findByProject_PmsProjectId(1L)).thenReturn(Arrays.asList());

        // Execute
        ResponseEntity<ApiResponse<DemandConflictValidationDTO>> response = 
            demandService.validateDemandConflicts(testDTO);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
        assertFalse(response.getBody().getData().isHasConflicts());
        assertTrue(response.getBody().getData().isCanSubmit());
        assertEquals("No conflicts detected. Demand can be submitted.", 
                    response.getBody().getData().getValidationMessage());
    }

    @Test
    void testValidateDemandConflicts_CapacityExceeded() {
        // Setup - Create existing demand that will cause capacity conflict
        com.entity.demand_entities.Demand existingDemand = new com.entity.demand_entities.Demand();
        existingDemand.setProject(testProject);
        existingDemand.setRole(testRole);
        existingDemand.setDemandStartDate(LocalDate.of(2026, 3, 1));
        existingDemand.setDemandEndDate(LocalDate.of(2026, 3, 31));
        existingDemand.setAllocationPercentage(60); // This will make total 110% with new demand
        existingDemand.setDemandStatus(com.entity_enums.demand_enums.DemandStatus.APPROVED);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(roleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))).thenReturn(Optional.of(testRole));
        when(demandRepository.findByProject_PmsProjectId(1L)).thenReturn(Arrays.asList(existingDemand));

        // Execute
        ResponseEntity<ApiResponse<DemandConflictValidationDTO>> response = 
            demandService.validateDemandConflicts(testDTO);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
        assertTrue(response.getBody().getData().isHasConflicts());
        assertFalse(response.getBody().getData().isCanSubmit()); // Should block submission
        
        List<DemandConflictValidationDTO.ConflictDetail> conflicts = response.getBody().getData().getConflicts();
        assertTrue(conflicts.stream().anyMatch(c -> "CAPACITY_EXCEEDED".equals(c.getConflictType())));
    }

    @Test
    void testValidateDemandConflicts_InsufficientJustification() {
        // Setup - Test with insufficient justification
        testDTO.setDemandJustification("Short"); // Less than 20 characters

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(roleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))).thenReturn(Optional.of(testRole));
        when(demandRepository.findByProject_PmsProjectId(1L)).thenReturn(Arrays.asList());

        // Execute
        ResponseEntity<ApiResponse<DemandConflictValidationDTO>> response = 
            demandService.validateDemandConflicts(testDTO);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isHasConflicts());
        assertFalse(response.getBody().getData().isCanSubmit());
        
        List<DemandConflictValidationDTO.ConflictDetail> conflicts = response.getBody().getData().getConflicts();
        assertTrue(conflicts.stream().anyMatch(c -> "INSUFFICIENT_JUSTIFICATION".equals(c.getConflictType())));
    }

    @Test
    void testValidateDemandConflicts_InvalidAllocation() {
        // Setup - Test with invalid allocation
        testDTO.setAllocationPercentage(110); // Over 100%

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(roleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))).thenReturn(Optional.of(testRole));
        when(demandRepository.findByProject_PmsProjectId(1L)).thenReturn(Arrays.asList());

        // Execute
        ResponseEntity<ApiResponse<DemandConflictValidationDTO>> response = 
            demandService.validateDemandConflicts(testDTO);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isHasConflicts());
        assertFalse(response.getBody().getData().isCanSubmit());
        
        List<DemandConflictValidationDTO.ConflictDetail> conflicts = response.getBody().getData().getConflicts();
        assertTrue(conflicts.stream().anyMatch(c -> "INVALID_ALLOCATION".equals(c.getConflictType())));
    }

    @Test
    void testValidateDemandConflicts_InvalidUUIDFormat() {
        // Setup - Test with valid UUID format
        testDTO.setDeliveryRole(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")); // Valid UUID

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(roleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))).thenReturn(Optional.of(testRole));
        when(demandRepository.findByProject_PmsProjectId(1L)).thenReturn(Arrays.asList());

        // Execute
        ResponseEntity<ApiResponse<DemandConflictValidationDTO>> response = 
            demandService.validateDemandConflicts(testDTO);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void testValidateDemandConflicts_ProjectNotFound() {
        // Setup
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // Execute
        ResponseEntity<ApiResponse<DemandConflictValidationDTO>> response = 
            demandService.validateDemandConflicts(testDTO);

        // Verify
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody().getData());
    }
}
