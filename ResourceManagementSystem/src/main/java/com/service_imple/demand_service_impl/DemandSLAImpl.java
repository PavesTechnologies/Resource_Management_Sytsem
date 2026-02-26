package com.service_imple.demand_service_impl;

import com.dto.ApiResponse;
import com.dto.demand_dto.DemandSlaResponseDTO;
import com.entity.demand_entities.DemandSLA;
import com.repo.demand_repo.DemandSLARepository;
import com.service_interface.demand_service_interface.DemandSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class DemandSLAImpl implements DemandSLAService {

    @Autowired
    private DemandSLARepository demandSLARepository;

    @Override
    public ResponseEntity<?> getAllDemandSLA() {
        List<DemandSLA> demandSLA = demandSLARepository.findAll().stream().toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Demand SLA fetched successfully", demandSLA));
    }

    @Override
    public ResponseEntity<?> getDemandSLAById(UUID demandId) {
        DemandSLA demandSLA = demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demandId).orElseThrow(()-> new RuntimeException("Demand Not Found!"));
        LocalDate now = LocalDate.now();

        long difference = ChronoUnit.DAYS.between(now, demandSLA.getDueAt());

        boolean breached = difference < 0;

        long remainingDays = breached ? 0 : difference;
        long overdueDays = breached ? Math.abs(difference) : 0;

        String priority = calculatePriority(demandSLA, difference, breached);

        DemandSlaResponseDTO response = DemandSlaResponseDTO.builder()
                .demandId(demandSLA.getDemand().getDemandId())
                .demandSlaId(demandSLA.getDemandSlaId())
                .slaType(demandSLA.getSlaType().name())
                .slaDurationDays(demandSLA.getSlaDurationDays())
                .warningThresholdDays(demandSLA.getWarningThresholdDays())
                .createdAt(demandSLA.getCreatedAt())
                .dueAt(demandSLA.getDueAt())
                .breached(breached)
                .remainingDays(remainingDays)
                .overdueDays(overdueDays)
                .priorityLevel(priority)
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Demand SLA fetched Successfully!", response));
    }

    private String calculatePriority(DemandSLA demandSLA,
                                     long difference,
                                     boolean breached) {

        if (breached) {
            return "CRITICAL";
        }

        if (difference <= demandSLA.getWarningThresholdDays()) {
            return "HIGH";
        }

        if (difference <= demandSLA.getSlaDurationDays() / 2) {
            return "MEDIUM";
        }

        return "LOW";
    }
}
