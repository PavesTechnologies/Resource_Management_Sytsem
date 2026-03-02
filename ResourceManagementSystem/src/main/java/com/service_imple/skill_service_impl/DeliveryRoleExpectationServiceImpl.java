package com.service_imple.skill_service_impl;

import com.dto.ApiResponse;
import com.dto.skill_dto.RoleExpectationRequest;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity.skill_entities.ProficiencyLevel;
import com.entity.skill_entities.Role;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.exception.skill_exceptions.DuplicateRoleExpectationException;
import com.exception.skill_exceptions.SkillValidationException;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.repo.skill_repo.RoleRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.DeliveryRoleExpectationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryRoleExpectationServiceImpl implements DeliveryRoleExpectationService {

    private final DeliveryRoleExpectationRepository expectationRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<String>> saveOrUpdateRoleExpectations(RoleExpectationRequest request) {
        log.info("Saving/updating role expectations for roleId: {}", request.getRoleId());

        validateRequest(request);

        Role role = getAndValidateRole(request.getRoleId());
        List<DeliveryRoleExpectation> existing = expectationRepository.findByRoleIdAndStatus(role.getId());
        boolean isUpdate = !existing.isEmpty();

        if (isUpdate) {
            log.info("UPDATE flow: Deleting existing expectations for roleId: {}", role.getId());
            expectationRepository.deleteByRoleId(role.getId());
        }

        List<DeliveryRoleExpectation> expectations = new ArrayList<>();

        for (RoleExpectationRequest.SkillExpectationDto skillDto : request.getSkills()) {
            saveSkillLevel(role, skillDto, expectations);
            
            if (skillDto.getSubSkills() != null && !skillDto.getSubSkills().isEmpty()) {
                for (RoleExpectationRequest.SubSkillExpectationDto subDto : skillDto.getSubSkills()) {
                    saveSubSkillLevel(role, skillDto, subDto, expectations);
                }
            }
        }

        expectationRepository.saveAll(expectations);

        String message = isUpdate ? 
            "Role expectations updated successfully" : 
            "Role expectations created successfully";
        
        HttpStatus status = isUpdate ? HttpStatus.OK : HttpStatus.CREATED;
        
        log.info("{} {} expectations for roleId: {}", 
            isUpdate ? "Updated" : "Created", expectations.size(), role.getId());

        return ResponseEntity.status(status)
            .body(ApiResponse.success(message));
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

    private void saveSkillLevel(Role role, RoleExpectationRequest.SkillExpectationDto skillDto, 
                           List<DeliveryRoleExpectation> expectations) {
        Skill skill = getAndValidateSkill(skillDto.getSkillId());
        ProficiencyLevel proficiencyLevel = getAndValidateProficiencyLevel(skillDto.getProficiencyId());

        DeliveryRoleExpectation expectation = DeliveryRoleExpectation.builder()
                .role(role)
                .skill(skill)
                .subSkill(null)
                .proficiencyLevel(proficiencyLevel)
                .mandatoryFlag(skillDto.getMandatoryFlag())
                .status("ACTIVE")
                .build();

        expectations.add(expectation);
    }

    private void saveSubSkillLevel(Role role, RoleExpectationRequest.SkillExpectationDto skillDto,
                                  RoleExpectationRequest.SubSkillExpectationDto subDto,
                                  List<DeliveryRoleExpectation> expectations) {
        Skill skill = getAndValidateSkill(skillDto.getSkillId());
        SubSkill subSkill = getAndValidateSubSkill(subDto.getSubSkillId(), skill);
        ProficiencyLevel proficiencyLevel = getAndValidateProficiencyLevel(subDto.getProficiencyId());

        DeliveryRoleExpectation expectation = DeliveryRoleExpectation.builder()
                .role(role)
                .skill(skill)
                .subSkill(subSkill)
                .proficiencyLevel(proficiencyLevel)
                .mandatoryFlag(subDto.getMandatoryFlag())
                .status("ACTIVE")
                .build();

        expectations.add(expectation);
    }

    private Role getAndValidateRole(UUID roleId) {
        log.debug("Looking for role with ID: {}", roleId);
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new SkillValidationException("Role not found with ID: " + roleId));
    }

    private void validateRequest(RoleExpectationRequest request) {
        if (request.getSkills() == null || request.getSkills().isEmpty()) {
            throw new SkillValidationException("Skills list cannot be empty");
        }

        // Check for duplicate skill IDs in request
        Set<UUID> skillIds = new HashSet<>();
        for (RoleExpectationRequest.SkillExpectationDto skill : request.getSkills()) {
            if (!skillIds.add(skill.getSkillId())) {
                throw new DuplicateRoleExpectationException(
                        "Duplicate skill ID found in request: " + skill.getSkillId());
            }

            // Check for duplicate subskill IDs within each skill
            if (skill.getSubSkills() != null) {
                Set<UUID> subSkillIds = new HashSet<>();
                for (RoleExpectationRequest.SubSkillExpectationDto sub : skill.getSubSkills()) {
                    if (!subSkillIds.add(sub.getSubSkillId())) {
                        throw new DuplicateRoleExpectationException(
                                "Duplicate subskill ID found in skill " + skill.getSkillId() + ": " + sub.getSubSkillId());
                    }
                }
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

        // For updates, we don't need to check for duplicates since we're soft deleting existing ones first
        // The duplicate check is handled at the request level in validateRequest()
    }

    private Skill getAndValidateSkill(UUID skillId) {
        log.debug("Looking for skill with ID: {}", skillId);
        Optional<Skill> skillOpt = skillRepository.findById(skillId);
        
        if (skillOpt.isEmpty()) {
            log.error("Skill not found with ID: {}", skillId);
            throw new SkillValidationException("Skill not found with ID: " + skillId);
        }

        Skill skill = skillOpt.get();
        log.debug("Found skill: {} with status: {}", skill.getName(), skill.getStatus());
        
        if (!"ACTIVE".equals(skill.getStatus())) {
            log.error("Skill is not active: {} (status: {})", skill.getName(), skill.getStatus());
            throw new SkillValidationException("Skill is not active: " + skill.getName());
        }

        return skill;
    }

    private SubSkill getAndValidateSubSkill(UUID subSkillId, Skill expectedSkill) {
        log.debug("Looking for subskill with ID: {}", subSkillId);
        Optional<SubSkill> subSkillOpt = subSkillRepository.findById(subSkillId);
        
        if (subSkillOpt.isEmpty()) {
            log.error("SubSkill not found with ID: {}", subSkillId);
            throw new SkillValidationException("SubSkill not found with ID: " + subSkillId);
        }

        SubSkill subSkill = subSkillOpt.get();
        log.debug("Found subskill: {} with status: {}, belongs to skill: {}", 
                subSkill.getName(), subSkill.getStatus(), subSkill.getSkill().getName());
        
        if (!"ACTIVE".equals(subSkill.getStatus())) {
            log.error("SubSkill is not active: {} (status: {})", subSkill.getName(), subSkill.getStatus());
            throw new SkillValidationException("SubSkill is not active: " + subSkill.getName());
        }

        if (!expectedSkill.getId().equals(subSkill.getSkill().getId())) {
            log.error("SubSkill {} does not belong to skill {}. SubSkill belongs to: {}", 
                    subSkill.getName(), expectedSkill.getName(), subSkill.getSkill().getName());
            throw new SkillValidationException(
                    "SubSkill does not belong to the specified skill. SubSkill: " + subSkill.getName() + 
                    ", Expected Skill: " + expectedSkill.getName());
        }

        return subSkill;
    }

    private ProficiencyLevel getAndValidateProficiencyLevel(UUID proficiencyLevelId) {
        log.debug("Looking for proficiency level with ID: {}", proficiencyLevelId);
        Optional<ProficiencyLevel> proficiencyLevelOpt = proficiencyLevelRepository.findById(proficiencyLevelId);
        
        if (proficiencyLevelOpt.isEmpty()) {
            log.error("ProficiencyLevel not found with ID: {}", proficiencyLevelId);
            throw new SkillValidationException("ProficiencyLevel not found with ID: " + proficiencyLevelId);
        }

        ProficiencyLevel proficiencyLevel = proficiencyLevelOpt.get();
        log.debug("Found proficiency level: {} with active flag: {}", 
                proficiencyLevel.getProficiencyName(), proficiencyLevel.getActiveFlag());
        
        if (!proficiencyLevel.getActiveFlag()) {
            log.error("ProficiencyLevel is not active: {} (active: {})", 
                    proficiencyLevel.getProficiencyName(), proficiencyLevel.getActiveFlag());
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
