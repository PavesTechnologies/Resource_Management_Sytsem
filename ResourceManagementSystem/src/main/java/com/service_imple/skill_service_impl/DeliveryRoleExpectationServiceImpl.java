package com.service_imple.skill_service_impl;

import com.dto.skill_dto.DeliveryRoleExpectationRequest;
import com.dto.skill_dto.DeliveryRoleExpectationResponse;
import com.dto.skill_dto.RoleExpectationWithMandatoryResponse;
import com.dto.skill_dto.RoleListResponse;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity.skill_entities.ProficiencyLevel;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.exception.skill_exceptions.DuplicateRoleExpectationException;
import com.exception.skill_exceptions.SkillValidationException;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.DeliveryRoleExpectationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryRoleExpectationServiceImpl implements DeliveryRoleExpectationService {

    private final DeliveryRoleExpectationRepository expectationRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;

    @Override
    @Transactional
    public DeliveryRoleExpectationResponse createOrUpdateRoleExpectations(DeliveryRoleExpectationRequest request) {
        log.info("Creating/updating role expectations for role: {}", request.getRoleName());

        validateRequest(request);

        String roleName = request.getRoleName() != null ? request.getRoleName().trim() : null;

        // Soft delete existing expectations for this role
        List<DeliveryRoleExpectation> existingExpectations = expectationRepository.findByRoleNameAndStatus(roleName);
        existingExpectations.forEach(expectation -> expectation.setStatus("INACTIVE"));
        expectationRepository.saveAll(existingExpectations);

        List<DeliveryRoleExpectation> expectations = new ArrayList<>();

        for (DeliveryRoleExpectationRequest.ExpectationDetail detail : request.getExpectations()) {
            validateExpectationDetail(detail, roleName);

            Skill skill = getAndValidateSkill(detail.getSkillId());
            SubSkill subSkill = detail.getSubSkillId() != null ? 
                    getAndValidateSubSkill(detail.getSubSkillId(), skill) : null;
            ProficiencyLevel proficiencyLevel = getAndValidateProficiencyLevel(detail.getProficiencyId());

            DeliveryRoleExpectation expectation = DeliveryRoleExpectation.builder()
                    .roleName(roleName)
                    .skill(skill)
                    .subSkill(subSkill)
                    .proficiencyLevel(proficiencyLevel)
                    .mandatoryFlag(detail.getMandatoryFlag())
                    .status("ACTIVE")
                    .build();

            expectations.add(expectation);
        }

        List<DeliveryRoleExpectation> savedExpectations = expectationRepository.saveAll(expectations);

        log.info("Successfully created/updated {} expectations for role: {}", 
                savedExpectations.size(), roleName);

        return groupExpectationsBySkill(savedExpectations);
    }

    @Override
    public DeliveryRoleExpectationResponse getRoleExpectations(String roleName) {
        log.info("Fetching expectations for role: {}", roleName);

        List<DeliveryRoleExpectation> expectations = expectationRepository.findByRoleNameAndStatus(roleName);

        if (expectations.isEmpty()) {
            log.warn("No expectations found for role: {}", roleName);
            return new DeliveryRoleExpectationResponse();
        }

        return groupExpectationsBySkill(expectations);
    }

    @Override
    public List<DeliveryRoleExpectationResponse> getAllRoleExpectations() {
        log.info("Fetching all role expectations");

        List<DeliveryRoleExpectation> allExpectations = expectationRepository.findAllActive();

        Map<String, List<DeliveryRoleExpectation>> groupedByRole = allExpectations.stream()
                .collect(Collectors.groupingBy(DeliveryRoleExpectation::getRoleName));

        return groupedByRole.entrySet().stream()
                .map(entry -> groupExpectationsBySkill(entry.getValue()))
                .sorted(Comparator.comparing(DeliveryRoleExpectationResponse::getRole))
                .collect(Collectors.toList());
    }

    @Override
    public RoleListResponse getAvailableRoles() {
        log.info("Fetching available roles");

        List<String> roles = expectationRepository.findDistinctRoleNames();

        RoleListResponse response = new RoleListResponse();
        response.setRoles(roles);

        return response;
    }

    @Override
    @Transactional
    public void deleteRoleExpectations(String roleName) {
        log.info("Soft deleting expectations for role: {}", roleName);

        List<DeliveryRoleExpectation> expectations = expectationRepository.findByRoleNameAndStatus(roleName);
        
        if (expectations.isEmpty()) {
            log.warn("No expectations found to delete for role: {}", roleName);
            return;
        }

        // Soft delete - set status to INACTIVE
        expectations.forEach(expectation -> expectation.setStatus("INACTIVE"));
        expectationRepository.saveAll(expectations);
        
        log.info("Successfully soft deleted {} expectations for role: {}", expectations.size(), roleName);
    }

    @Override
    public boolean hasRoleExpectations(String roleName) {
        long count = expectationRepository.countByRoleNameAndStatus(roleName);
        return count > 0;
    }

    private void validateRequest(DeliveryRoleExpectationRequest request) {
        if (request.getExpectations() == null || request.getExpectations().isEmpty()) {
            throw new SkillValidationException("At least one expectation is required");
        }

        // Check for duplicates within the same request
        Set<String> skillSubSkillCombinations = new HashSet<>();
        for (DeliveryRoleExpectationRequest.ExpectationDetail detail : request.getExpectations()) {
            String key = detail.getSkillId() + "_" + 
                    (detail.getSubSkillId() != null ? detail.getSubSkillId() : "NULL");
            
            if (!skillSubSkillCombinations.add(key)) {
                throw new DuplicateRoleExpectationException(
                        "Duplicate skill-subskill combination found in request: " + key);
            }
        }
    }

    private void validateExpectationDetail(DeliveryRoleExpectationRequest.ExpectationDetail detail, String roleName) {
        if (detail.getSkillId() == null) {
            throw new SkillValidationException("Skill ID is required");
        }

        if (detail.getProficiencyId() == null) {
            throw new SkillValidationException("Proficiency ID is required");
        }

        if (detail.getMandatoryFlag() == null) {
            throw new SkillValidationException("Each skill must be marked as Mandatory or Optional");
        }

        // Check for existing duplicates
        boolean exists;
        if (detail.getSubSkillId() != null) {
            exists = expectationRepository.existsByRoleNameAndSkill_IdAndSubSkill_Id(
                    roleName, detail.getSkillId(), detail.getSubSkillId());
        } else {
            exists = expectationRepository.existsByRoleNameAndSkill_IdAndSubSkill_IdIsNull(
                    roleName, detail.getSkillId());
        }

        if (exists) {
            throw new DuplicateRoleExpectationException(
                    "Role expectation already exists for role: " + roleName + 
                    ", skill: " + detail.getSkillId() + 
                    ", subSkill: " + detail.getSubSkillId());
        }
    }

    private Skill getAndValidateSkill(UUID skillId) {
        Optional<Skill> skillOpt = skillRepository.findById(skillId);
        
        if (skillOpt.isEmpty()) {
            throw new SkillValidationException("Skill not found with ID: " + skillId);
        }

        Skill skill = skillOpt.get();
        
        if (!"ACTIVE".equals(skill.getStatus())) {
            throw new SkillValidationException("Skill is not active: " + skill.getName());
        }

        return skill;
    }

    private SubSkill getAndValidateSubSkill(UUID subSkillId, Skill expectedSkill) {
        Optional<SubSkill> subSkillOpt = subSkillRepository.findById(subSkillId);
        
        if (subSkillOpt.isEmpty()) {
            throw new SkillValidationException("SubSkill not found with ID: " + subSkillId);
        }

        SubSkill subSkill = subSkillOpt.get();
        
        if (!"ACTIVE".equals(subSkill.getStatus())) {
            throw new SkillValidationException("SubSkill is not active: " + subSkill.getName());
        }

        if (!expectedSkill.getId().equals(subSkill.getSkill().getId())) {
            throw new SkillValidationException(
                    "SubSkill does not belong to the specified skill. SubSkill: " + subSkill.getName() + 
                    ", Expected Skill: " + expectedSkill.getName());
        }

        return subSkill;
    }

    private ProficiencyLevel getAndValidateProficiencyLevel(UUID proficiencyLevelId) {
        Optional<ProficiencyLevel> proficiencyLevelOpt = proficiencyLevelRepository.findById(proficiencyLevelId);
        
        if (proficiencyLevelOpt.isEmpty()) {
            throw new SkillValidationException("ProficiencyLevel not found with ID: " + proficiencyLevelId);
        }

        ProficiencyLevel proficiencyLevel = proficiencyLevelOpt.get();
        
        if (!proficiencyLevel.getActiveFlag()) {
            throw new SkillValidationException("ProficiencyLevel is not active: " + proficiencyLevel.getProficiencyName());
        }

        return proficiencyLevel;
    }

    private DeliveryRoleExpectationResponse groupExpectationsBySkill(List<DeliveryRoleExpectation> expectations) {
        DeliveryRoleExpectationResponse response = new DeliveryRoleExpectationResponse();
        
        if (expectations.isEmpty()) {
            return response;
        }

        response.setRole(expectations.get(0).getRoleName());
        response.setDev_role_id(expectations.get(0).getId());

        Map<String, List<DeliveryRoleExpectation>> groupedBySkill = expectations.stream()
                .collect(Collectors.groupingBy(e -> e.getSkill().getName()));

        List<DeliveryRoleExpectationResponse.SkillRequirement> skillRequirements = new ArrayList<>();

        for (Map.Entry<String, List<DeliveryRoleExpectation>> entry : groupedBySkill.entrySet()) {
            DeliveryRoleExpectationResponse.SkillRequirement skillReq = 
                    new DeliveryRoleExpectationResponse.SkillRequirement();
            skillReq.setSkill(entry.getKey());

            List<DeliveryRoleExpectationResponse.RequirementDetail> requirements = entry.getValue().stream()
                    .map(e -> {
                        DeliveryRoleExpectationResponse.RequirementDetail detail = 
                                new DeliveryRoleExpectationResponse.RequirementDetail();
                        detail.setSubSkill(e.getSubSkill() != null ? e.getSubSkill().getName() : null);
                        detail.setProficiency(e.getProficiencyLevel().getProficiencyName());
                        detail.setMandatoryFlag(e.getMandatoryFlag());
                        return detail;
                    })
                    .collect(Collectors.toList());

            skillReq.setRequirements(requirements);
            skillRequirements.add(skillReq);
        }

        response.setSkills(skillRequirements.stream()
                .sorted(Comparator.comparing(DeliveryRoleExpectationResponse.SkillRequirement::getSkill))
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    public RoleExpectationWithMandatoryResponse getRoleExpectationsWithMandatory(String roleName) {
        log.info("Fetching role expectations with mandatory/optional separation for role: {}", roleName);

        List<DeliveryRoleExpectation> expectations = expectationRepository.findByRoleNameAndStatus(roleName);

        if (expectations.isEmpty()) {
            log.warn("No expectations found for role: {}", roleName);
            return new RoleExpectationWithMandatoryResponse();
        }

        RoleExpectationWithMandatoryResponse response = new RoleExpectationWithMandatoryResponse();
        response.setRoleName(roleName);
        response.setDev_role_id(expectations.get(0).getId());

        List<RoleExpectationWithMandatoryResponse.SkillRequirement> mandatorySkills = new ArrayList<>();
        List<RoleExpectationWithMandatoryResponse.SkillRequirement> optionalSkills = new ArrayList<>();

        for (DeliveryRoleExpectation expectation : expectations) {
            RoleExpectationWithMandatoryResponse.SkillRequirement skillReq = 
                    new RoleExpectationWithMandatoryResponse.SkillRequirement();
            skillReq.setSkill(expectation.getSkill().getName());
            skillReq.setSubSkill(expectation.getSubSkill() != null ? expectation.getSubSkill().getName() : null);
            skillReq.setProficiency(expectation.getProficiencyLevel().getProficiencyName());

            if (Boolean.TRUE.equals(expectation.getMandatoryFlag())) {
                mandatorySkills.add(skillReq);
            } else {
                optionalSkills.add(skillReq);
            }
        }

        response.setMandatorySkills(mandatorySkills.stream()
                .sorted(Comparator.comparing(RoleExpectationWithMandatoryResponse.SkillRequirement::getSkill))
                .collect(Collectors.toList()));

        response.setOptionalSkills(optionalSkills.stream()
                .sorted(Comparator.comparing(RoleExpectationWithMandatoryResponse.SkillRequirement::getSkill))
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    public boolean isResourceEligibleForRole(String roleName, List<UUID> resourceSkillIds) {
        log.info("Checking resource eligibility for role: {}", roleName);

        List<DeliveryRoleExpectation> roleExpectations = expectationRepository.findByRoleNameAndStatus(roleName);
        
        // Get all mandatory skill requirements for this role
        List<UUID> mandatorySkillIds = roleExpectations.stream()
                .filter(expectation -> Boolean.TRUE.equals(expectation.getMandatoryFlag()))
                .map(expectation -> expectation.getSkill().getId())
                .collect(Collectors.toList());

        // Check if resource has all mandatory skills
        boolean hasAllMandatorySkills = resourceSkillIds.containsAll(mandatorySkillIds);

        log.info("Resource eligibility for role {}: {} (Required: {}, Has: {})", 
                roleName, hasAllMandatorySkills, mandatorySkillIds, resourceSkillIds);

        return hasAllMandatorySkills;
    }
}
