package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.roleoff_repo.RoleOffEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AllocationClosureScheduler {
    private final AllocationRepository allocationRepository;
    private final AllocationServiceImple allocationService;
    private final RoleOffEventRepository roleOffEventRepository;

//    @Scheduled(cron = "0 0 1 * * *")
//    @Scheduled(fixedRate = 30000)
@Scheduled(cron = "0 0 1 * * *")
    public void closeExpiredAllocations() {

        LocalDate today = LocalDate.now();

        // Process expired allocations based on end date
        List<ResourceAllocation> expiredAllocations =
                allocationRepository.findExpiredAllocations(today);

        for(ResourceAllocation allocation : expiredAllocations){
            closeAllocationWithAudit(allocation, "SYSTEM", "Allocation expired - end date reached", today);
        }

        // Process role-off events with effective date today
        List<RoleOffEvent> roleOffsToday = roleOffEventRepository.findApprovedRoleOffsForToday(today);

        for(RoleOffEvent roleOffEvent : roleOffsToday){
            processRoleOffClosure(roleOffEvent, today);
        }
    }

    private void closeAllocationWithAudit(ResourceAllocation allocation, String closedBy, String closureReason, LocalDate closureDate) {
        allocation.setAllocationStatus(AllocationStatus.ENDED);
        allocation.setClosedBy(closedBy);
        allocation.setClosedAt(LocalDateTime.now());
        allocation.setClosureReason(closureReason);
        
        if (closureDate != null && closureDate.isBefore(allocation.getAllocationEndDate())) {
            allocation.setAllocationEndDate(closureDate);
        }

        allocationRepository.save(allocation);
        allocationService.updateAvailabilityLedgerForAllocation(allocation);
    }

    private void processRoleOffClosure(RoleOffEvent roleOffEvent, LocalDate today) {
        // Find active allocation for this role-off
        Optional<ResourceAllocation> activeAllocation = allocationRepository
                .findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                        roleOffEvent.getProject().getPmsProjectId(),
                        roleOffEvent.getResource().getResourceId(),
                        AllocationStatus.ACTIVE
                );

        activeAllocation.ifPresent(allocation -> {
            // Handle partial/early closure based on role-off effective date
            String closureReason = String.format("Role-off effective: %s - %s", 
                    roleOffEvent.getRoleOffReasonEnum(), 
                    roleOffEvent.getRoleOffReason());
            
            closeAllocationWithAudit(allocation, "SYSTEM", closureReason, roleOffEvent.getEffectiveRoleOffDate());
        });

        // Update role-off event status
        roleOffEvent.setRoleOffStatus(RoleOffStatus.FULFILLED);
        roleOffEventRepository.save(roleOffEvent);
    }
}
