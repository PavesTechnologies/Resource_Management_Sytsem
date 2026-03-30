package com.service_imple.bench_service_impl;

import com.dto.bench_dto.MatchResponse;
import com.entity.resource_entities.Resource;
import com.entity.demand_entities.Demand;
import com.entity.skill_entities.ResourceSkill;
import com.service_interface.bench_service_interface.BenchDemandMatchingService;
import com.service_interface.demand_service_interface.DemandService;
import com.repo.bench_repo.BenchDetectionRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchDemandMatchingServiceImpl implements BenchDemandMatchingService {

    private final BenchDetectionRepository benchDetectionRepository;
    private final DemandService demandService;
    private final ResourceSkillRepository resourceSkillRepository;

    @Override
    public List<MatchResponse> getMatches() {
        log.info("Getting bench-demand matches");
        
        List<Resource> benchResources = benchDetectionRepository.findAllBenchResources();
        log.info("Found {} bench resources", benchResources.size());
        if (benchResources.isEmpty()) {
            log.warn("No bench resources found!");
            return new ArrayList<>();
        }
        
        List<Demand> demands = demandService.getOpenDemands();
        log.info("Found {} open demands", demands.size());
        if (demands.isEmpty()) {
            log.warn("No open demands found!");
            return new ArrayList<>();
        }
        
        List<MatchResponse> results = new ArrayList<>();

        for (Demand demand : demands) {
            log.info("Processing demand: {} (ID: {})", demand.getDemandName(), demand.getDemandId());
            for (Resource resource : benchResources) {
                log.info("Evaluating resource: {} (ID: {})", resource.getFullName(), resource.getResourceId());
                double score = calculateMatch(resource, demand);
                log.info("Resource {} vs Demand {}: score = {}", resource.getFullName(), demand.getDemandName(), score);
                
                if (score > 30) { // temporarily lowered threshold for debugging
                    results.add(buildMatchResponse(resource, demand, score));
                    log.info("Added match: {} -> {} (score: {})", resource.getFullName(), demand.getDemandName(), score);
                } else {
                    log.info("Score too low: {} (threshold: 30)", score);
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

    private double calculateMatch(Resource resource, Demand demand) {
        // 3.1 Skill Matching (50% weight)
        double skillScore = calculateSkillScore(resource, demand);
        
        // 3.2 Experience Matching (30% weight)
        double expScore = calculateExperienceScore(resource, demand);
        
        // 3.3 Availability Matching (20% weight)
        double availabilityScore = calculateAvailabilityScore(resource, demand);
        
        // 3.4 Final Score
        return (skillScore * 0.5) + (expScore * 0.3) + (availabilityScore * 0.2);
    }

    private double calculateSkillScore(Resource resource, Demand demand) {
        // Get resource skills through ResourceSkill repository
        List<ResourceSkill> resourceSkills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resource.getResourceId());
        
        List<String> resourceSkillNames = resourceSkills.stream()
                .map(rs -> rs.getSkill().getName())
                .collect(Collectors.toList());
        
        List<String> demandSkillNames = demand.getRequiredSkills().stream()
                .map(skill -> skill.getName())
                .collect(Collectors.toList());
        
        // If no required skills, check delivery role
        if (demandSkillNames.isEmpty() && demand.getRole() != null && demand.getRole().getSkill() != null) {
            // Convert delivery role skill to skill name for matching
            demandSkillNames = List.of(demand.getRole().getSkill().getName());
            log.info("No required skills found, using delivery role skill: {}", demandSkillNames);
        }
        
        log.info("Resource {} skills: {}", resource.getFullName(), resourceSkillNames);
        log.info("Demand {} required skills: {}", demand.getDemandName(), demandSkillNames);
        
        long matchedSkills = resourceSkillNames.stream()
                .filter(demandSkillNames::contains)
                .count();
        
        double score = demandSkillNames.isEmpty() ? 0 : (matchedSkills / (double) demandSkillNames.size()) * 100;
        log.info("Skill match: {}/{} = {}", matchedSkills, demandSkillNames.size(), score);
        
        return score;
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
        // Simple availability check - assume bench resources are available
        // In real implementation, check resource.availableDate vs demand.startDate
        return 100; // Bench resources are generally available
    }

    private MatchResponse buildMatchResponse(Resource resource, Demand demand, double score) {
        List<String> matchedSkills = getMatchedSkills(resource, demand);
        
        return MatchResponse.builder()
                .resourceId(resource.getResourceId())
                .resourceName(resource.getFullName())
                .resourceExperience(resource.getExperiance() != null ? resource.getExperiance().intValue() : 0)
                .matchScore(Math.round(score))
                .matchedSkills(matchedSkills)
                .availability("Available") // Bench resources are available
                .demandId(demand.getDemandId())
                .demandName(demand.getDemandName())
                .build();
    }

    private List<String> getMatchedSkills(Resource resource, Demand demand) {
        // Get resource skills through ResourceSkill repository
        List<ResourceSkill> resourceSkills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resource.getResourceId());
        
        List<String> resourceSkillNames = resourceSkills.stream()
                .map(rs -> rs.getSkill().getName())
                .collect(Collectors.toList());
        
        List<String> demandSkillNames = demand.getRequiredSkills().stream()
                .map(skill -> skill.getName())
                .collect(Collectors.toList());
        
        // If no required skills, check delivery role (same logic as calculateSkillScore)
        if (demandSkillNames.isEmpty() && demand.getRole() != null && demand.getRole().getSkill() != null) {
            demandSkillNames = List.of(demand.getRole().getSkill().getName());
        }
        
        return resourceSkillNames.stream()
                .filter(demandSkillNames::contains)
                .collect(Collectors.toList());
    }
}
