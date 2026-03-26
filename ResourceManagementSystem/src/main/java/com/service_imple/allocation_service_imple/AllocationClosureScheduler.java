package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.roleoff_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.roleoff_enums.RoleOffStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.roleoff_repo.RoleOffEventRepository;
import com.service_imple.skill_service_impl.ResourceSkillUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AllocationClosureScheduler {
    private final AllocationRepository allocationRepository;
    private final AllocationServiceImple allocationService;
    private final RoleOffEventRepository roleOffEventRepository;
    private final ResourceSkillUsageService resourceSkillUsageService;

//    @Scheduled(cron = "0 0 1 * * *")
//    @Scheduled(fixedRate = 30000)
@Scheduled(cron = "0 0 1 * * *")
    public void closeExpiredAllocations() {

        LocalDate today = LocalDate.now();

        // Process expired allocations based on end date
        List<ResourceAllocation> expiredAllocations =
                allocationRepository.findExpiredAllocations(today);

        // Collect skill update requests for batch processing
        List<ResourceSkillUsageService.RoleOffSkillUpdateRequest> skillUpdateRequests = new ArrayList<>();

        for(ResourceAllocation allocation : expiredAllocations){
            closeAllocationWithAudit(allocation, "SYSTEM", "Allocation expired - end date reached", today);
            
            // Add to batch for skill updates
            skillUpdateRequests.add(new ResourceSkillUsageService.RoleOffSkillUpdateRequest(
                allocation.getResource(),
                allocation.getProject(),
                null, // No role-off effective date for expiry
                allocation.getAllocationEndDate(),
                allocation.getProject().getEndDate().toLocalDate()
            ));
        }

        // Process role-off events with effective date today
        List<RoleOffEvent> roleOffsToday = roleOffEventRepository.findApprovedRoleOffsForToday(today);

        for(RoleOffEvent roleOffEvent : roleOffsToday){
            processRoleOffClosure(roleOffEvent, today);
            
            // Add to batch for skill updates
            skillUpdateRequests.add(new ResourceSkillUsageService.RoleOffSkillUpdateRequest(
                roleOffEvent.getResource(),
                roleOffEvent.getProject(),
                roleOffEvent.getEffectiveRoleOffDate(),
                roleOffEvent.getAllocation().getAllocationEndDate(),
                roleOffEvent.getProject().getEndDate().toLocalDate()
            ));
        }

        // Batch update skill lastUsedDate for performance
        if (!skillUpdateRequests.isEmpty()) {
            try {
                resourceSkillUsageService.batchUpdateSkillLastUsedDates(skillUpdateRequests);
            } catch (Exception e) {
                System.err.println("Failed to batch update skill lastUsedDates: " + e.getMessage());
            }
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
        // Note: Skill lastUsedDate updates are handled in batch at the end of closeExpiredAllocations()
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
