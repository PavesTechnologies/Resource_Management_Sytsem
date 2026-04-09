package com.service_imple.bench_service_impl;

import com.dto.bench_dto.MatchResponse;
import com.dto.allocation_dto.SkillGapAnalysisResponseDTO;
import com.entity.resource_entities.Resource;
import com.entity.demand_entities.Demand;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.service_interface.bench_service_interface.BenchDemandMatchingService;
import com.service_interface.demand_service_interface.DemandService;
import com.service_imple.allocation_service_imple.SkillGapAnalysisService;
import com.repo.bench_repo.BenchDetectionRepository;
import com.repo.allocation_repo.AllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchDemandMatchingServiceImpl implements BenchDemandMatchingService {

    private final BenchDetectionRepository benchDetectionRepository;
    private final DemandService demandService;
    private final AllocationRepository allocationRepository;
    private final SkillGapAnalysisService skillGapAnalysisService;

    @Override
    public List<MatchResponse> getMatches() {
        log.info("Getting bench-demand matches using comprehensive skill analysis (APPROVED demands only)");

        // Fetch bench resources for matching
        List<Resource> benchResources = benchDetectionRepository.findAllBenchResources();

        if (benchResources.isEmpty()) {
            log.warn("No valid bench resources found!");
            return new ArrayList<>();
        }

        List<Demand> demands = demandService.getApprovedDemands();
        log.info("Found {} approved demands for matching", demands.size());

        if (demands.isEmpty()) {
            log.warn("No approved demands found!");
            return new ArrayList<>();
        }
        
        // Additional validation to ensure all demands are APPROVED
        List<Demand> verifiedApprovedDemands = demands.stream()
            .filter(demand -> {
                boolean isApproved = demand.getDemandStatus() == com.entity_enums.demand_enums.DemandStatus.APPROVED;
                if (!isApproved) {
                    log.warn("Filtering out non-approved demand: {} (Status: {})", 
                        demand.getDemandName(), demand.getDemandStatus());
                }
                return isApproved;
            })
            .collect(java.util.stream.Collectors.toList());
        
        log.info("Verified {} approved demands after filtering", verifiedApprovedDemands.size());

        List<MatchResponse> results = new ArrayList<>();

        for (Demand demand : verifiedApprovedDemands) {
            log.info("Processing demand: {} (ID: {}) - Status: {}", 
                    demand.getDemandName(), demand.getDemandId(), demand.getDemandStatus());

            for (Resource resource : benchResources) {
                log.info("Evaluating resource: {} (ID: {})", resource.getFullName(), resource.getResourceId());

                try {
                    // Use existing comprehensive skill gap analysis
                    SkillGapAnalysisResponseDTO skillGapResult = skillGapAnalysisService.performSkillGapAnalysis(demand, resource.getResourceId());
                    
                    // Calculate overall match score (skill gap + experience + availability)
                    double overallScore = calculateOverallMatch(resource, demand, skillGapResult);

                    log.info("Resource {} vs Demand {}: skillGap={}%, overallScore={}%",
                            resource.getFullName(), demand.getDemandName(), 
                            skillGapResult.getMatchPercentage(), overallScore);

                    if (overallScore > 30) { // threshold - back to 30%
                        results.add(buildMatchResponse(resource, demand, overallScore, skillGapResult));
                        log.info("Added match: {} -> {} (score: {})",
                                resource.getFullName(), demand.getDemandName(), overallScore);
                    } else {
                        log.info("Score too low: {} (threshold: 30)", overallScore);
                    }
                } catch (Exception e) {
                    log.error("Error analyzing match for resource {} and demand {}: {}", 
                             resource.getResourceId(), demand.getDemandId(), e.getMessage());
                    continue;
                }
            }
        }

        // Sort by best matches first
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        log.info("Found {} matches above threshold", results.size());
        return results;
    }

    @Override
    public List<MatchResponse> getMatches(String skill, Integer minExp) {
        log.info("Getting filtered matches - skill: {}, minExp: {}", skill, minExp);
        
        List<MatchResponse> allMatches = getMatches();
        log.info("Total matches found: {}", allMatches.size());
        
        List<MatchResponse> filteredMatches = allMatches.stream()
                .filter(match -> {
                    boolean skillMatch = skill == null || match.getMatchedSkills().contains(skill);
                    boolean expMatch = minExp == null || match.getResourceExperience() >= minExp;
                    log.info("Filtering match {} - skillMatch: {}, expMatch: {}", 
                        match.getResourceName(), skillMatch, expMatch);
                    return skillMatch && expMatch;
                })
                .collect(Collectors.toList());
        
        log.info("Filtered matches: {}", filteredMatches.size());
        return filteredMatches;
    }

    /**
     * Calculate overall match score combining skill gap analysis with experience and availability
     */
    private double calculateOverallMatch(Resource resource, Demand demand, SkillGapAnalysisResponseDTO skillGapResult) {
        // 1. Skill Matching Score (50% weight) - from comprehensive skill gap analysis
        double skillScore = skillGapResult.getMatchPercentage();
        
        // 2. Experience Matching (30% weight)
        double expScore = calculateExperienceScore(resource, demand);
        
        // 3. Availability Matching (20% weight)
        double availabilityScore = calculateAvailabilityScore(resource, demand);
        
        // 4. Risk Adjustment Factor (penalty for high risk allocations)
        double riskAdjustment = calculateRiskAdjustment(skillGapResult);
        
        // Final Score with risk adjustment
        double finalScore = (skillScore * 0.5) + (expScore * 0.3) + (availabilityScore * 0.2);
        return finalScore * riskAdjustment;
    }

    private double calculateExperienceScore(Resource resource, Demand demand) {
        Long resourceExp = resource.getExperiance();
        Double minExp = demand.getMinExp();
        
        if (resourceExp != null && minExp != null && resourceExp.doubleValue() >= minExp) {
            return 100;
        } else {
            return 50; // partial match
        }
    }

    private double calculateAvailabilityScore(Resource resource, Demand demand) {
        // Check if resource is available for demand start date
        LocalDate demandStartDate = demand.getDemandStartDate();
        LocalDate today = LocalDate.now();
        
        // Resource must be in bench status and allocation allowed
        if (!resource.getAllocationAllowed() || !resource.getActiveFlag()) {
            return 0; // Not available for allocation
        }
        
        // Check if resource has any active allocations that overlap with demand period
        List<ResourceAllocation> activeAllocations = allocationRepository
                .findByResource_ResourceIdAndAllocationStatus(resource.getResourceId(), AllocationStatus.ACTIVE);
        
        boolean hasOverlappingAllocation = activeAllocations.stream()
                .anyMatch(allocation -> 
                    !(allocation.getAllocationEndDate().isBefore(demandStartDate) || 
                      allocation.getAllocationStartDate().isAfter(demand.getDemandEndDate())));
        
        if (hasOverlappingAllocation) {
            return 0; // Has overlapping allocation
        }
        
        // Check if resource is immediately available (bench resources should be available from today)
        if (demandStartDate.isBefore(today)) {
            return 50; // Demand start date is in the past, partial availability
        }
        
        // Full availability for bench resources with demand start date today or future
        return 100;
    }

    /**
     * Calculate risk adjustment factor based on skill gap analysis risk level
     */
    private double calculateRiskAdjustment(SkillGapAnalysisResponseDTO skillGapResult) {
        String riskLevel = skillGapResult.getRiskLevel();
        
        switch (riskLevel.toUpperCase()) {
            case "HIGH":
                return 0.7; // 30% penalty for high risk
            case "MEDIUM":
                return 0.85; // 15% penalty for medium risk
            case "LOW":
            default:
                return 1.0; // No penalty for low risk
        }
    }

    private MatchResponse buildMatchResponse(Resource resource, Demand demand, double overallScore, 
                                          SkillGapAnalysisResponseDTO skillGapResult) {
        List<String> matchedSkills = extractMatchedSkills(skillGapResult);
        String availabilityStatus = determineAvailabilityStatus(resource, demand);
        
        return MatchResponse.builder()
                .resourceId(resource.getResourceId())
                .resourceName(resource.getFullName())
                .resourceExperience(resource.getExperiance() != null ? resource.getExperiance().intValue() : 0)
                .matchScore(Math.round(overallScore))
                .matchedSkills(matchedSkills)
                .availability(availabilityStatus)
                .demandId(demand.getDemandId())
                .demandName(demand.getDemandName())
                .build();
    }

    private String determineAvailabilityStatus(Resource resource, Demand demand) {
        LocalDate demandStartDate = demand.getDemandStartDate();
        LocalDate today = LocalDate.now();
        
        // Check resource basic availability
        if (!resource.getAllocationAllowed() || !resource.getActiveFlag()) {
            return "Not Available";
        }
        
        // Check for overlapping allocations
        List<ResourceAllocation> activeAllocations = allocationRepository
                .findByResource_ResourceIdAndAllocationStatus(resource.getResourceId(), AllocationStatus.ACTIVE);
        
        boolean hasOverlappingAllocation = activeAllocations.stream()
                .anyMatch(allocation -> 
                    !(allocation.getAllocationEndDate().isBefore(demandStartDate) || 
                      allocation.getAllocationStartDate().isAfter(demand.getDemandEndDate())));
        
        if (hasOverlappingAllocation) {
            return "Partially Available";
        }
        
        // Check demand start date
        if (demandStartDate.isBefore(today)) {
            return "Available Immediately";
        } else if (demandStartDate.isEqual(today)) {
            return "Available Today";
        } else {
            return "Available from " + demandStartDate;
        }
    }

    /**
     * Extract matched skills from skill gap analysis result
     */
    private List<String> extractMatchedSkills(SkillGapAnalysisResponseDTO skillGapResult) {
        List<String> matchedSkills = new ArrayList<>();
        
        // Add matched skills from skill comparisons
        skillGapResult.getSkillComparisons().stream()
                .filter(comparison -> !"GAP".equals(comparison.getStatus()))
                .forEach(comparison -> {
                    if (comparison.getSubSkillName() != null) {
                        matchedSkills.add(comparison.getSkillName() + " - " + comparison.getSubSkillName());
                    } else {
                        matchedSkills.add(comparison.getSkillName());
                    }
                });
        
        // Add matched certificates
        skillGapResult.getCertificateComparisons().stream()
                .filter(comparison -> !"GAP".equals(comparison.getStatus()))
                .forEach(comparison -> matchedSkills.add("Cert: " + comparison.getCertificateName()));
        
        return matchedSkills;
    }
}
