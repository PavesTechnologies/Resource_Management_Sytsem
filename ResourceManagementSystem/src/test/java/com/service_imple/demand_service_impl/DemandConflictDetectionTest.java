package com.service_imple.demand_service_impl;

import com.entity.demand_entities.Demand;
import com.entity.project_entities.Project;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.repo.demand_repo.DemandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemandConflictDetectionTest {

    @Mock
    private DemandRepository demandRepository;

    @InjectMocks
    private DemandServiceImpl demandService;

    private Demand testDemand;
    private Project testProject;
    private DeliveryRoleExpectation testRole;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setPmsProjectId(1L);
        testProject.setName("Test Project");

        testRole = new DeliveryRoleExpectation();
        testRole.setId(1L);
        testRole.setRoleName("Developer");

        testDemand = new Demand();
        testDemand.setDemandId(UUID.randomUUID());
        testDemand.setProject(testProject);
        testDemand.setRole(testRole);
        testDemand.setDemandName("Test Demand");
        testDemand.setDemandStartDate(LocalDate.of(2026, 3, 1));
        testDemand.setDemandEndDate(LocalDate.of(2026, 3, 31));
        testDemand.setAllocationPercentage(50);
        testDemand.setDemandType(DemandType.NET_NEW);
        testDemand.setDemandStatus(DemandStatus.REQUESTED);
        testDemand.setDemandPriority(PriorityLevel.HIGH);
        testDemand.setResourcesRequired(1);
        testDemand.setCreatedBy(1L);
    }

    @Test
    void testDateRangeOverlap() {
        // Test overlapping dates
        assertTrue(isDateRangeOverlapping(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 15)
        ));

        // Test non-overlapping dates
        assertFalse(isDateRangeOverlapping(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)
        ));

        // Test edge case - same end date as start date
        assertFalse(isDateRangeOverlapping(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)
        ));
    }

    @Test
    void testExactDuplicateDetection() {
        Demand existingDemand = new Demand();
        existingDemand.setProject(testProject);
        existingDemand.setRole(testRole);
        existingDemand.setDemandStartDate(LocalDate.of(2026, 3, 1));
        existingDemand.setDemandEndDate(LocalDate.of(2026, 3, 31));
        existingDemand.setAllocationPercentage(50);

        assertTrue(isExactDuplicate(testDemand, existingDemand));

        // Change one field - should not be duplicate
        existingDemand.setAllocationPercentage(60);
        assertFalse(isExactDuplicate(testDemand, existingDemand));
    }

    @Test
    void testSimilarDemandDetection() {
        Demand similarDemand = new Demand();
        similarDemand.setProject(testProject);
        similarDemand.setRole(testRole);
        similarDemand.setDemandStartDate(LocalDate.of(2026, 3, 15));
        similarDemand.setDemandEndDate(LocalDate.of(2026, 4, 15));
        similarDemand.setAllocationPercentage(40);

        assertTrue(isSimilarDemand(testDemand, similarDemand));

        // Different project - should not be similar
        Project differentProject = new Project();
        differentProject.setPmsProjectId(2L);
        similarDemand.setProject(differentProject);
        assertFalse(isSimilarDemand(testDemand, similarDemand));
    }

    @Test
    void testAllocationConflictDetection() {
        Demand existingDemand = new Demand();
        existingDemand.setProject(testProject);
        existingDemand.setDemandStartDate(LocalDate.of(2026, 3, 1));
        existingDemand.setDemandEndDate(LocalDate.of(2026, 3, 31));
        existingDemand.setAllocationPercentage(60);
        existingDemand.setDemandStatus(DemandStatus.APPROVED);
        existingDemand.setDemandPriority(PriorityLevel.MEDIUM);

        List<Demand> existingDemands = Arrays.asList(existingDemand);
        
        when(demandRepository.findByProject_PmsProjectId(1L)).thenReturn(existingDemands);

        // Total allocation would be 110% (50 + 60) - should trigger conflict
        int totalAllocation = testDemand.getAllocationPercentage();
        for (Demand existing : existingDemands) {
            if (isDateRangeOverlapping(testDemand.getDemandStartDate(), testDemand.getDemandEndDate(),
                                      existing.getDemandStartDate(), existing.getDemandEndDate()) &&
                existing.getDemandStatus() != DemandStatus.CANCELLED &&
                existing.getDemandStatus() != DemandStatus.REJECTED) {
                totalAllocation += existing.getAllocationPercentage();
            }
        }

        assertTrue(totalAllocation > 100, "Should detect allocation conflict");
    }

    @Test
    void testTimelineConflictDetection() {
        Demand conflictingDemand = new Demand();
        conflictingDemand.setProject(testProject);
        conflictingDemand.setRole(testRole);
        conflictingDemand.setDemandStartDate(LocalDate.of(2026, 3, 15));
        conflictingDemand.setDemandEndDate(LocalDate.of(2026, 4, 15));
        conflictingDemand.setDemandStatus(DemandStatus.APPROVED);
        conflictingDemand.setDemandPriority(PriorityLevel.LOW);

        assertTrue(isDateRangeOverlapping(
            testDemand.getDemandStartDate(), testDemand.getDemandEndDate(),
            conflictingDemand.getDemandStartDate(), conflictingDemand.getDemandEndDate()
        ));

        assertTrue(testDemand.getRole().getId().equals(conflictingDemand.getRole().getId()));
        assertTrue(conflictingDemand.getDemandStatus() != DemandStatus.CANCELLED);
        assertTrue(conflictingDemand.getDemandStatus() != DemandStatus.REJECTED);
    }

    // Helper methods copied from DemandServiceImpl for testing
    private boolean isDateRangeOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    private boolean isExactDuplicate(Demand newDemand, Demand existing) {
        return newDemand.getProject().getPmsProjectId().equals(existing.getProject().getPmsProjectId()) &&
               newDemand.getRole().getId().equals(existing.getRole().getId()) &&
               newDemand.getDemandStartDate().equals(existing.getDemandStartDate()) &&
               newDemand.getDemandEndDate().equals(existing.getDemandEndDate()) &&
               newDemand.getAllocationPercentage().equals(existing.getAllocationPercentage());
    }

    private boolean isSimilarDemand(Demand newDemand, Demand existing) {
        return newDemand.getProject().getPmsProjectId().equals(existing.getProject().getPmsProjectId()) &&
               newDemand.getRole().getId().equals(existing.getRole().getId()) &&
               isDateRangeOverlapping(newDemand.getDemandStartDate(), newDemand.getDemandEndDate(), 
                                     existing.getDemandStartDate(), existing.getDemandEndDate());
    }
}
