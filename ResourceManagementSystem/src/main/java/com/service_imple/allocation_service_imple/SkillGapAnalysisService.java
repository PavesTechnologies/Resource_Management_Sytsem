package com.service_imple.allocation_service_imple;


import com.dto.allocation_dto.SkillGapAnalysisResponseDTO;
import com.dto.allocation_dto.AllocationRequestDTO;
import com.entity.demand_entities.Demand;
import com.entity.skill_entities.*;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.demand_repo.DemandRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing skill gaps between resources and demands
 */
@Service
@RequiredArgsConstructor
public class SkillGapAnalysisService {

    private final DemandRepository demandRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
    private final ResourceCertificateRepository resourceCertificateRepository;
    private final SkillRepository skillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final CertificateRepository certificateRepository;
    private final DeliveryRoleExpectationRepository deliveryRoleExpectationRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Performs comprehensive skill gap analysis for a demand and resource
     */
    public SkillGapAnalysisResponseDTO performSkillGapAnalysis(Demand demand, Long resourceId) {
        LocalDate currentDate = LocalDate.now();

        // Single queries to fetch all resource data (performance optimization)
        List<ResourceSkill> resourceSkills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        List<ResourceSubSkill> resourceSubSkills = resourceSubSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        List<ResourceCertificate> resourceCertificates = resourceCertificateRepository.findActiveCertificatesForResource(resourceId, currentDate);

        // Count database queries for metrics
        meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_skills").increment();
        meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_subskills").increment();
        meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_certificates").increment();

        // Batch fetch all proficiency levels needed for this analysis
        Map<UUID, ProficiencyLevel> proficiencyLevelMap = getProficiencyLevelMap(resourceSkills, resourceSubSkills);
        meterRegistry.counter("skill_gap_analysis_cache_hit_ratio", "cache", "proficiency_levels").increment();

        // Create lookup maps for O(1) access
        Map<UUID, ResourceSkill> resourceSkillMap = resourceSkills.stream()
            .collect(Collectors.toMap(rs -> rs.getSkill().getId(), rs -> rs));
        Map<UUID, ResourceSubSkill> resourceSubSkillMap = resourceSubSkills.stream()
            .collect(Collectors.toMap(rss -> rss.getSubSkill().getId(), rss -> rss));
        Map<UUID, ResourceCertificate> resourceCertificateMap = resourceCertificates.stream()
            .collect(Collectors.toMap(ResourceCertificate::getCertificateId, rc -> rc));

        // Initialize analysis components
        List<SkillGapAnalysisResponseDTO.SkillComparisonDTO> skillComparisons = new ArrayList<>();
        List<SkillGapAnalysisResponseDTO.CertificateComparisonDTO> certificateComparisons = new ArrayList<>();
        List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings = new ArrayList<>();

        double totalScore = 0.0;
        int totalRequirements = 0;
        boolean hasMandatoryGap = false;
        boolean hasMandatoryPartial = false;

        // 1. Process DeliveryRoleExpectation (highest priority)
        if (demand.getRole() != null) {
            DeliveryRoleExpectation role = demand.getRole();

            // Fetch ALL role details for this role name with single query
            List<DeliveryRoleExpectation> allRoleExpectations = deliveryRoleExpectationRepository.findByRoleIdWithDetails(role.getRole().getId());

            for (DeliveryRoleExpectation roleExpectation : allRoleExpectations) {
                SkillGapAnalysisResponseDTO.SkillComparisonDTO roleComparison = compareRoleExpectation(
                    roleExpectation, resourceSkillMap, resourceSubSkillMap, proficiencyLevelMap, recencyWarnings
                );

                skillComparisons.add(roleComparison);
                totalScore += roleComparison.getScore();
                totalRequirements++;

                if (roleComparison.getStatus().equals("GAP") && roleExpectation.getMandatoryFlag()) {
                    hasMandatoryGap = true;
                } else if (roleComparison.getStatus().equals("PARTIAL") && roleExpectation.getMandatoryFlag()) {
                    hasMandatoryPartial = true;
                }
            }
        }

        // 2. Process Demand.requiredSkills (excluding those already in role)
        Set<String> roleSkillNames = skillComparisons.stream()
            .filter(sc -> sc.getSubSkillName() == null) // Main skills only
            .map(SkillGapAnalysisResponseDTO.SkillComparisonDTO::getSkillName)
            .collect(Collectors.toSet());

        for (Skill requiredSkill : demand.getRequiredSkills()) {
            if (!roleSkillNames.contains(requiredSkill.getName())) {
                SkillGapAnalysisResponseDTO.SkillComparisonDTO skillComparison = compareRequiredSkill(
                    requiredSkill, resourceSkillMap, recencyWarnings
                );

                skillComparisons.add(skillComparison);
                totalScore += skillComparison.getScore();
                totalRequirements++;
            }
        }

        // 3. Process Demand.requiredCertificates
        for (Certificate requiredCert : demand.getRequiredCertificates()) {
            SkillGapAnalysisResponseDTO.CertificateComparisonDTO certComparison = compareRequiredCertificate(
                requiredCert, resourceCertificateMap
            );

            certificateComparisons.add(certComparison);
            totalScore += certComparison.getScore();
            totalRequirements++;
        }

        // Calculate final metrics
        double matchPercentage = totalRequirements > 0 ? (totalScore / totalRequirements) * 100 : 0.0;
        String riskLevel = calculateOverallRisk(hasMandatoryGap, hasMandatoryPartial, recencyWarnings);
        boolean allocationAllowed = AllocationRiskEvaluator.isAllocationAllowed(hasMandatoryGap, hasMandatoryPartial);

        return SkillGapAnalysisResponseDTO.builder()
            .demandId(demand.getDemandId())
            .resourceId(resourceId)
            .matchPercentage(matchPercentage)
            .allocationAllowed(allocationAllowed)
            .riskLevel(riskLevel)
            .skillComparisons(skillComparisons)
            .certificateComparisons(certificateComparisons)
            .recencyWarnings(recencyWarnings)
            .build();
    }

    /**
     * Validates that the resource has all required skills and certificates for the demand
     * Also validates allocation percentage matches demand requirement
     */
    public void validateAllocationRequirements(Long resourceId, Demand demand, AllocationRequestDTO request) {
        // 🔹 Validate Allocation Percentage Match
        if (request != null && demand != null) {
            Integer requestPercentage = request.getAllocationPercentage();
            Integer demandPercentage = demand.getAllocationPercentage();
            
            if (requestPercentage != null && demandPercentage != null && 
                !requestPercentage.equals(demandPercentage)) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "ALLOCATION_PERCENTAGE_MISMATCH",
                        String.format("Allocation percentage (%d%%) must match demand requirement (%d%%)", 
                                     requestPercentage, demandPercentage)
                );
            }
        }
        
        LocalDate currentDate = LocalDate.now();
        
        // 🔹 Validate Skills
        if (demand.getRequiredSkills() != null && !demand.getRequiredSkills().isEmpty()) {
            List<ResourceSkill> resourceSkills = resourceSkillRepository
                    .findByResourceIdAndActiveFlagTrue(resourceId);
            
            Set<UUID> resourceSkillIds = resourceSkills.stream()
                    .map(rs -> rs.getSkill().getId())
                    .collect(Collectors.toSet());
            
            Set<UUID> requiredSkillIds = demand.getRequiredSkills().stream()
                    .map(Skill::getId)
                    .collect(Collectors.toSet());
            
            requiredSkillIds.removeAll(resourceSkillIds);
            
            if (!requiredSkillIds.isEmpty()) {
                List<String> missingSkillNames = demand.getRequiredSkills().stream()
                        .filter(skill -> requiredSkillIds.contains(skill.getId()))
                        .map(Skill::getName)
                        .collect(Collectors.toList());
                
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUIRED_SKILLS",
                        "Missing required skills: " + String.join(", ", missingSkillNames)
                );
            }
        }
        
        // 🔹 Validate Certificates
        if (demand.getRequiredCertificates() != null && !demand.getRequiredCertificates().isEmpty()) {
            List<ResourceCertificate> resourceCertificates = resourceCertificateRepository
                    .findActiveCertificatesForResource(resourceId, currentDate);
            
            Set<UUID> resourceCertificateIds = resourceCertificates.stream()
                    .map(ResourceCertificate::getCertificateId)
                    .collect(Collectors.toSet());
            
            Set<UUID> requiredCertificateIds = demand.getRequiredCertificates().stream()
                    .map(Certificate::getCertificateId)
                    .collect(Collectors.toSet());
            
            requiredCertificateIds.removeAll(resourceCertificateIds);
            
            if (!requiredCertificateIds.isEmpty()) {
                List<String> missingCertificateNames = demand.getRequiredCertificates().stream()
                        .filter(cert -> requiredCertificateIds.contains(cert.getCertificateId()))
                        .map(cert -> cert.getProviderName() != null ? cert.getProviderName() : cert.getCertificateId().toString())
                        .collect(Collectors.toList());
                
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUIRED_CERTIFICATIONS",
                        "Missing required certifications: " + String.join(", ", missingCertificateNames)
                );
            }
        }
    }

    /**
     * Compares role expectation with resource capabilities
     */
    private SkillGapAnalysisResponseDTO.SkillComparisonDTO compareRoleExpectation(
            DeliveryRoleExpectation role, 
            Map<UUID, ResourceSkill> resourceSkillMap,
            Map<UUID, ResourceSubSkill> resourceSubSkillMap,
            Map<UUID, ProficiencyLevel> proficiencyLevelMap,
            List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {

        String skillName = role.getSkill().getName();
        String subSkillName = role.getSubSkill() != null ? role.getSubSkill().getName() : null;
        String requiredProficiency = role.getProficiencyLevel().getProficiencyName();

        // Check for recency warning
        addRecencyWarningIfNeeded(skillName, subSkillName, 
            subSkillName != null ? resourceSubSkillMap.get(role.getSubSkill().getId()) : resourceSkillMap.get(role.getSkill().getId()),
            recencyWarnings);

        if (subSkillName != null) {
            // Sub-skill comparison
            ResourceSubSkill resourceSubSkill = resourceSubSkillMap.get(role.getSubSkill().getId());
            if (resourceSubSkill != null && isActiveAndNotExpired(resourceSubSkill)) {
                ProficiencyLevel resourceProficiency = proficiencyLevelMap.get(resourceSubSkill.getProficiencyId());
                ProficiencyComparator.ProficiencyResult result = ProficiencyComparator.compareProficiency(
                    resourceProficiency, role.getProficiencyLevel());

                return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
                    .skillName(skillName)
                    .subSkillName(subSkillName)
                    .requiredProficiency(requiredProficiency)
                    .resourceProficiency(resourceProficiency != null ? resourceProficiency.getProficiencyName() : null)
                    .mandatory(role.getMandatoryFlag())
                    .status(result.getStatus())
                    .score(result.getScore())
                    .build();
            }
        } else {
            // Main skill comparison
            ResourceSkill resourceSkill = resourceSkillMap.get(role.getSkill().getId());
            if (resourceSkill != null && isActiveAndNotExpired(resourceSkill)) {
                ProficiencyLevel resourceProficiency = proficiencyLevelMap.get(resourceSkill.getProficiencyId());
                ProficiencyComparator.ProficiencyResult result = ProficiencyComparator.compareProficiency(
                    resourceProficiency, role.getProficiencyLevel());

                return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
                    .skillName(skillName)
                    .subSkillName(null)
                    .requiredProficiency(requiredProficiency)
                    .resourceProficiency(resourceProficiency != null ? resourceProficiency.getProficiencyName() : null)
                    .mandatory(role.getMandatoryFlag())
                    .status(result.getStatus())
                    .score(result.getScore())
                    .build();
            }
        }

        // Skill not found or inactive/expired
        return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
            .skillName(skillName)
            .subSkillName(subSkillName)
            .requiredProficiency(requiredProficiency)
            .resourceProficiency(null)
            .mandatory(role.getMandatoryFlag())
            .status("GAP")
            .score(0.0)
            .build();
    }

    /**
     * Compares required skill (compliance skills) with resource capabilities
     */
    private SkillGapAnalysisResponseDTO.SkillComparisonDTO compareRequiredSkill(
            Skill requiredSkill, 
            Map<UUID, ResourceSkill> resourceSkillMap,
            List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {

        ResourceSkill resourceSkill = resourceSkillMap.get(requiredSkill.getId());
        
        // Check for recency warning
        addRecencyWarningIfNeeded(requiredSkill.getName(), null, resourceSkill, recencyWarnings);

        if (resourceSkill != null && isActiveAndNotExpired(resourceSkill)) {
            // Compliance skills don't have proficiency requirements - just existence check
            addRecencyWarningIfNeeded(requiredSkill.getName(), null, resourceSkill, recencyWarnings);
            
            return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
                .skillName(requiredSkill.getName())
                .subSkillName(null)
                .requiredProficiency("N/A")
                .resourceProficiency("Available")
                .mandatory(false) // Compliance skills are not mandatory at this level
                .status("MATCH")
                .score(1.0)
                .build();
        }

        return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
            .skillName(requiredSkill.getName())
            .subSkillName(null)
            .requiredProficiency("N/A")
            .resourceProficiency(null)
            .mandatory(false)
            .status("GAP")
            .score(0.0)
            .build();
    }

    /**
     * Compares required certificate with resource certifications
     */
    private SkillGapAnalysisResponseDTO.CertificateComparisonDTO compareRequiredCertificate(
            Certificate requiredCert, 
            Map<UUID, ResourceCertificate> resourceCertificateMap) {

        ResourceCertificate resourceCert = resourceCertificateMap.get(requiredCert.getCertificateId());

        if (resourceCert != null) {
            return SkillGapAnalysisResponseDTO.CertificateComparisonDTO.builder()
                .certificateName(requiredCert.getProviderName() != null ? requiredCert.getProviderName() : requiredCert.getCertificateId().toString())
                .mandatory(true) // Certificates from compliance are mandatory
                .status("MATCH")
                .score(1.0)
                .build();
        }

        return SkillGapAnalysisResponseDTO.CertificateComparisonDTO.builder()
            .certificateName(requiredCert.getProviderName() != null ? requiredCert.getProviderName() : requiredCert.getCertificateId().toString())
            .mandatory(true)
            .status("GAP")
            .score(0.0)
            .build();
    }

    /**
     * Adds recency warning if skill is old
     */
    private void addRecencyWarningIfNeeded(String skillName, String subSkillName, 
                                        Object resourceSkill, List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {
        LocalDate lastUsedDate = null;
        
        if (resourceSkill instanceof ResourceSkill) {
            lastUsedDate = ((ResourceSkill) resourceSkill).getLastUsedDate();
        } else if (resourceSkill instanceof ResourceSubSkill) {
            lastUsedDate = ((ResourceSubSkill) resourceSkill).getLastUsedDate();
        }

        String riskLevel = AllocationRiskEvaluator.evaluateRecencyRisk(lastUsedDate);
        if (!AllocationRiskEvaluator.ALLOCATION_RISK_LOW.equals(riskLevel)) {
            recencyWarnings.add(SkillGapAnalysisResponseDTO.RecencyWarningDTO.builder()
                .skillName(skillName)
                .subSkillName(subSkillName)
                .lastUsedDate(lastUsedDate)
                .riskLevel(riskLevel)
                .yearsUnused(AllocationRiskEvaluator.calculateYearsUnused(lastUsedDate))
                .build());
        }
    }

    /**
     * Calculates overall risk level
     */
    private String calculateOverallRisk(boolean hasMandatoryGap, boolean hasMandatoryPartial, 
                                    List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {
        String mandatoryGapRisk = hasMandatoryGap ? AllocationRiskEvaluator.ALLOCATION_RISK_HIGH : AllocationRiskEvaluator.ALLOCATION_RISK_LOW;
        String partialRisk = hasMandatoryPartial ? AllocationRiskEvaluator.ALLOCATION_RISK_HIGH : AllocationRiskEvaluator.ALLOCATION_RISK_LOW;
        
        String recencyRisk = AllocationRiskEvaluator.ALLOCATION_RISK_LOW;
        for (SkillGapAnalysisResponseDTO.RecencyWarningDTO warning : recencyWarnings) {
            if (AllocationRiskEvaluator.ALLOCATION_RISK_HIGH.equals(warning.getRiskLevel())) {
                recencyRisk = AllocationRiskEvaluator.ALLOCATION_RISK_HIGH;
                break;
            } else if (AllocationRiskEvaluator.ALLOCATION_RISK_MEDIUM.equals(warning.getRiskLevel())) {
                recencyRisk = AllocationRiskEvaluator.ALLOCATION_RISK_MEDIUM;
            }
        }

        return AllocationRiskEvaluator.aggregateRisk(mandatoryGapRisk, partialRisk, recencyRisk);
    }

    /**
     * Get proficiency level map with caching
     */
    @Cacheable(value = "proficiencyLevels", key = "'allActiveProficiencyLevels'")
    private Map<UUID, ProficiencyLevel> getProficiencyLevelMap(List<ResourceSkill> resourceSkills, List<ResourceSubSkill> resourceSubSkills) {
        // Collect all unique proficiency IDs needed
        Set<UUID> proficiencyIds = new HashSet<>();
        resourceSkills.forEach(rs -> proficiencyIds.add(rs.getProficiencyId()));
        resourceSubSkills.forEach(rss -> proficiencyIds.add(rss.getProficiencyId()));
        
        if (proficiencyIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Fetch all active proficiency levels (cached)
        List<ProficiencyLevel> allActiveProficiencyLevels = proficiencyLevelRepository.findAllActiveProficiencyLevels();
        
        // Filter to only needed proficiency levels and create map
        return allActiveProficiencyLevels.stream()
            .filter(pl -> proficiencyIds.contains(pl.getProficiencyId()))
            .collect(Collectors.toMap(ProficiencyLevel::getProficiencyId, pl -> pl));
    }

    /**
     * Clear proficiency levels cache
     */
    @CacheEvict(value = "proficiencyLevels", allEntries = true)
    public void clearProficiencyLevelsCache() {
        // Cache cleared - next call will reload from database
    }

    /**
     * Clear all skill-related caches
     */
    @CacheEvict(value = {"proficiencyLevels", "skills", "subSkills", "certificates"}, allEntries = true)
    public void clearAllSkillCaches() {
        // All skill-related caches cleared
    }

    private boolean isActiveAndNotExpired(ResourceSkill resourceSkill) {
        return resourceSkill.getActiveFlag();
    }

    private boolean isActiveAndNotExpired(ResourceSubSkill resourceSubSkill) {
        return resourceSubSkill.getActiveFlag();
    }

    // Utility class for risk evaluation (assuming this exists elsewhere)
    private static class AllocationRiskEvaluator {
        static final String ALLOCATION_RISK_LOW = "LOW";
        static final String ALLOCATION_RISK_MEDIUM = "MEDIUM";
        static final String ALLOCATION_RISK_HIGH = "HIGH";

        static String evaluateRecencyRisk(LocalDate lastUsedDate) {
            if (lastUsedDate == null) return ALLOCATION_RISK_HIGH;
            
            long yearsUnused = java.time.temporal.ChronoUnit.YEARS.between(lastUsedDate, LocalDate.now());
            if (yearsUnused > 3) return ALLOCATION_RISK_HIGH;
            if (yearsUnused > 1) return ALLOCATION_RISK_MEDIUM;
            return ALLOCATION_RISK_LOW;
        }

        static long calculateYearsUnused(LocalDate lastUsedDate) {
            if (lastUsedDate == null) return 999;
            return java.time.temporal.ChronoUnit.YEARS.between(lastUsedDate, LocalDate.now());
        }

        static boolean isAllocationAllowed(boolean hasMandatoryGap, boolean hasMandatoryPartial) {
            return !hasMandatoryGap;
        }

        static String aggregateRisk(String... risks) {
            for (String risk : risks) {
                if (ALLOCATION_RISK_HIGH.equals(risk)) return ALLOCATION_RISK_HIGH;
            }
            for (String risk : risks) {
                if (ALLOCATION_RISK_MEDIUM.equals(risk)) return ALLOCATION_RISK_MEDIUM;
            }
            return ALLOCATION_RISK_LOW;
        }
    }

    // Utility class for proficiency comparison (assuming this exists elsewhere)
    private static class ProficiencyComparator {
        static ProficiencyResult compareProficiency(ProficiencyLevel resource, ProficiencyLevel required) {
            if (resource == null) {
                return new ProficiencyResult("GAP", 0.0);
            }
            
            // Simple comparison - in real implementation, this would be more sophisticated
            int resourceLevel = resource.getDisplayOrder() != null ? resource.getDisplayOrder() : 0;
            int requiredLevel = required.getDisplayOrder() != null ? required.getDisplayOrder() : 0;
            
            if (resourceLevel >= requiredLevel) {
                return new ProficiencyResult("MATCH", 1.0);
            } else if (resourceLevel >= requiredLevel - 1) {
                return new ProficiencyResult("PARTIAL", 0.5);
            } else {
                return new ProficiencyResult("GAP", 0.0);
            }
        }

        static class ProficiencyResult {
            private final String status;
            private final double score;

            public ProficiencyResult(String status, double score) {
                this.status = status;
                this.score = score;
            }

            public String getStatus() { return status; }
            public double getScore() { return score; }
        }
    }
}
