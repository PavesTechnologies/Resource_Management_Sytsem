package com.service_imple.skill_service_impl;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.skill_dto.AllocationValidationRequestDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.repo.allocation_repo.AllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AllocationService {
    private final AllocationRuleEngine ruleEngine;
    private final AllocationRepository allocationRepository;

    @Transactional
    public String allocate(AllocationRequestDTO dto) {

        // 🔴 FINAL GOVERNANCE GATE
        AllocationValidationRequestDTO validationDto = mapToValidationRequest(dto);
        ruleEngine.validateAllocation(validationDto);

        allocationRepository.save(new ResourceAllocation());

        return "Allocation successful";
    }

    private AllocationValidationRequestDTO mapToValidationRequest(AllocationRequestDTO dto) {
        return new AllocationValidationRequestDTO(
            dto.getResourceId(),
            null, // skillRequirements - needs to be populated based on demand/project
            null  // requiredCertificationSkillIds - needs to be populated based on demand/project
        );
    }
}
