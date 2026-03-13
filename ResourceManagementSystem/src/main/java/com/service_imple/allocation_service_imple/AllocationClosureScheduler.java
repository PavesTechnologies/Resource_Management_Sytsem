package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.repo.allocation_repo.AllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AllocationClosureScheduler {
    private final AllocationRepository allocationRepository;
    private final AllocationServiceImple allocationService;

//    @Scheduled(cron = "0 0 1 * * *")
//    @Scheduled(fixedRate = 30000)
@Scheduled(cron = "0 0 1 * * *")
    public void closeExpiredAllocations() {

//        System.out.println("Scheduler started: checking expired allocations");

        LocalDate today = LocalDate.now();

        List<ResourceAllocation> allocations =
                allocationRepository.findExpiredAllocations(today);
//        System.out.println("Expired allocations found: " + allocations.size());

        for(ResourceAllocation allocation : allocations){

            allocation.setAllocationStatus(AllocationStatus.ENDED);

            allocationRepository.save(allocation);

            allocationService.updateAvailabilityLedgerForAllocation(allocation);
//            System.out.println("Closed allocation: " + allocation.getAllocationId());

        }
    }
}
