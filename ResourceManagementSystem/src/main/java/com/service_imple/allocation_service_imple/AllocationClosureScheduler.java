package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.roleoff_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.roleoff_enums.RoleOffStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.roleoff_repo.RoleOffEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationClosureScheduler {

    private final AllocationRepository allocationRepository;
    private final RoleOffEventRepository roleOffEventRepository;

//    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    @Transactional
    public void processAutoClosures() {
        try {
            LocalDate today = LocalDate.now();
            
            // Get expired allocations
            List<ResourceAllocation> expiredAllocations = allocationRepository.findExpiredAllocations(today);
            
            for (ResourceAllocation allocation : expiredAllocations) {
                // Check if there's a pending role-off event for this allocation
                RoleOffEvent roleOffEvent = roleOffEventRepository.findByAllocation_AllocationId(allocation.getAllocationId());
                
                if (roleOffEvent != null && 
                    roleOffEvent.getRmApproved() != null && roleOffEvent.getRmApproved() &&
                    (roleOffEvent.getDlApproved() == null || !roleOffEvent.getDlApproved()) &&
                    (roleOffEvent.getRoleOffStatus() == RoleOffStatus.PENDING || roleOffEvent.getRoleOffStatus() == RoleOffStatus.APPROVED)) {
                    
                    // Auto-fulfill the role-off event (scheduler processed it)
                    roleOffEvent.setDlApproved(true);
                    roleOffEvent.setDlActionDate(today);
                    roleOffEvent.setRoleOffStatus(RoleOffStatus.FULFILLED);
                    roleOffEventRepository.save(roleOffEvent);
                    
                    log.info("Auto-fulfilled role-off event {} for allocation {}", roleOffEvent.getId(), allocation.getAllocationId());
                }
                
                // Close the allocation
                allocation.setAllocationStatus(AllocationStatus.ROLLED_OFF);
                allocationRepository.save(allocation);
            }
            
            if (!expiredAllocations.isEmpty()) {
                log.info("Auto-closed {} expired allocations", expiredAllocations.size());
            }
        } catch (Exception e) {
            log.error("Failed to process auto-closures: {}", e.getMessage());
        }
    }
}
