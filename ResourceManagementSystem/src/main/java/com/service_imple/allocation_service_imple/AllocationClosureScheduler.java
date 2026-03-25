package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.roleoff_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.roleoff_enums.RoleOffStatus;
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

    private void processRoleOffClosure(RoleOffEvent event, LocalDate today) {

        // 🚨 MUST CHECK DL APPROVAL
        if (!Boolean.TRUE.equals(event.getDlApproved())) {
            return;
        }

        Optional<ResourceAllocation> allocation = allocationRepository
                .findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                        event.getProject().getPmsProjectId(),
                        event.getResource().getResourceId(),
                        AllocationStatus.ACTIVE
                );

        allocation.ifPresent(a -> {
            closeAllocationWithAudit(a, "SYSTEM",
                    "Role-off: " + event.getRoleOffReason(),
                    event.getEffectiveRoleOffDate());
        });

        event.setRoleOffStatus(RoleOffStatus.FULFILLED);
        roleOffEventRepository.save(event);
    }
}
