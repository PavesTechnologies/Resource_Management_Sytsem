package com.service_imple.allocation_service_imple;

import com.repo.allocation_repo.AllocationRepository;
import com.entity_enums.allocation_enums.AllocationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationClosureScheduler {

    private final AllocationRepository allocationRepository;

//    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    @Transactional
    public void processAutoClosures() {
        try {
            LocalDate today = LocalDate.now();
            int updated = allocationRepository.autoCloseAllocations(today, AllocationStatus.ENDED);
            if (updated > 0) {
                log.info("Auto-closed {} expired allocations", updated);
            }
        } catch (Exception e) {
            log.error("Failed to process auto-closures: {}", e.getMessage());
        }
    }
}
