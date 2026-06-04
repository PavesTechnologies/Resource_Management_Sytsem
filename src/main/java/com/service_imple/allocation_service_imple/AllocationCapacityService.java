package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Service for validating resource capacity using timeline segmentation algorithm
 */
@Service
@RequiredArgsConstructor
public class AllocationCapacityService {

    /**
     * Validates timeline capacity using advanced segmentation algorithm
     * 
     * This algorithm divides the timeline into segments based on allocation boundaries
     * and checks capacity constraints for each segment individually.
     * 
     * @param existingAllocations Existing allocations for the resource
     * @param requestStart Requested allocation start date
     * @param requestEnd Requested allocation end date
     * @param requestPercentage Requested allocation percentage
     * @return true if capacity is available, false if exceeded
     */
    public boolean validateTimelineCapacity(
            List<ResourceAllocation> existingAllocations,
            LocalDate requestStart,
            LocalDate requestEnd,
            int requestPercentage) {
        
        // Handle edge cases
        if (requestStart == null || requestEnd == null || existingAllocations == null) {
            return true; // Skip validation if data is incomplete
        }
        
        // STEP 1: Build timeline boundaries
        // Collect all critical dates that define timeline segments
        TreeSet<LocalDate> boundaries = new TreeSet<>();
        
        // Add request allocation boundaries
        boundaries.add(requestStart);
        boundaries.add(requestEnd.plusDays(1)); // End date is inclusive, so next day creates boundary
        
        // Add existing allocation boundaries
        for (ResourceAllocation allocation : existingAllocations) {
            if (allocation.getAllocationStartDate() != null) {
                boundaries.add(allocation.getAllocationStartDate());
            }
            if (allocation.getAllocationEndDate() != null) {
                boundaries.add(allocation.getAllocationEndDate().plusDays(1));
            }
        }
        
        // STEP 2: Convert to sorted list and create segments
        List<LocalDate> sortedBoundaries = new ArrayList<>(boundaries);
        
        // STEP 3: Evaluate each timeline segment
        for (int i = 0; i < sortedBoundaries.size() - 1; i++) {
            LocalDate segmentStart = sortedBoundaries.get(i);
            LocalDate segmentEnd = sortedBoundaries.get(i + 1).minusDays(1); // Convert back to inclusive end
            
            // Skip if segment doesn't overlap with request window
            if (segmentEnd.isBefore(requestStart) || segmentStart.isAfter(requestEnd)) {
                continue;
            }
            
            // Calculate total allocation percentage for this segment
            int totalAllocation = requestPercentage; // Start with requested allocation
            
            // Add overlapping existing allocations
            for (ResourceAllocation allocation : existingAllocations) {
                if (isAllocationOverlappingSegment(allocation, segmentStart, segmentEnd)) {
                    totalAllocation += allocation.getAllocationPercentage();
                }
            }
            
            // STEP 4: Check capacity constraint
            if (totalAllocation > 100) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks if an allocation overlaps with a specific timeline segment
     * 
     * @param allocation The allocation to check
     * @param segmentStart Start of the timeline segment
     * @param segmentEnd End of the timeline segment
     * @return true if allocation overlaps with the segment
     */
    private boolean isAllocationOverlappingSegment(
            ResourceAllocation allocation, 
            LocalDate segmentStart, 
            LocalDate segmentEnd) {
        
        if (allocation.getAllocationStartDate() == null || allocation.getAllocationEndDate() == null) {
            return false;
        }
        
        LocalDate allocStart = allocation.getAllocationStartDate();
        LocalDate allocEnd = allocation.getAllocationEndDate();
        
        // Check overlap: allocation starts before segment ends AND allocation ends after segment starts
        return !allocStart.isAfter(segmentEnd) && !allocEnd.isBefore(segmentStart);
    }

    public int calculateMaxTimelineAllocation(
            List<ResourceAllocation> existingAllocations,
            LocalDate requestStart,
            LocalDate requestEnd,
            int requestPercentage) {

        TreeSet<LocalDate> boundaries = new TreeSet<>();

        boundaries.add(requestStart);
        boundaries.add(requestEnd.plusDays(1));

        for (ResourceAllocation allocation : existingAllocations) {
            boundaries.add(allocation.getAllocationStartDate());
            boundaries.add(allocation.getAllocationEndDate().plusDays(1));
        }

        List<LocalDate> sortedBoundaries = new ArrayList<>(boundaries);

        int maxAllocation = 0;

        for (int i = 0; i < sortedBoundaries.size() - 1; i++) {

            LocalDate segmentStart = sortedBoundaries.get(i);
            LocalDate segmentEnd = sortedBoundaries.get(i + 1).minusDays(1);

            if (segmentEnd.isBefore(requestStart) || segmentStart.isAfter(requestEnd)) {
                continue;
            }

            int segmentAllocation = requestPercentage;

            for (ResourceAllocation allocation : existingAllocations) {

                if (!allocation.getAllocationStartDate().isAfter(segmentEnd) &&
                        !allocation.getAllocationEndDate().isBefore(segmentStart)) {

                    if (allocation.getAllocationStatus() == AllocationStatus.ACTIVE ||
                            allocation.getAllocationStatus() == AllocationStatus.PLANNED) {

                        segmentAllocation += allocation.getAllocationPercentage();
                    }
                }
            }

            maxAllocation = Math.max(maxAllocation, segmentAllocation);
        }

        return maxAllocation;
    }
}
