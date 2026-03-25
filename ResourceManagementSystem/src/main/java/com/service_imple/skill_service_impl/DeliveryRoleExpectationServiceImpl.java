package com.service_imple.skill_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.*;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity.skill_entities.ProficiencyLevel;
import com.entity.skill_entities.Role;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.DuplicateRoleExpectationException;
import com.global_exception_handler.SkillValidationException;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.repo.skill_repo.RoleRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.DeliveryRoleExpectationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryRoleExpectationServiceImpl implements DeliveryRoleExpectationService {

    private final DeliveryRoleExpectationRepository expectationRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<String>> saveOrUpdateRoleExpectations(RoleExpectationRequest request) {

        validateRequest(request);

        Role role = getAndValidateRole(request.getRoleId());
        List<DeliveryRoleExpectation> existing = expectationRepository.findByRoleIdAndStatus(role.getId());
        boolean isUpdate = !existing.isEmpty();

        if (isUpdate) {
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

        return ResponseEntity.status(status)
            .body(ApiResponse.success(message));
    }

    @Override
    @Transactional
    public DeliveryRoleExpectationResponse createRoleExpectations(DeliveryRoleExpectationRequest request) {

        validateDeliveryRoleRequest(request);

        // Create or get role by name
        Role role = getOrCreateRoleByName(request.getRoleName());
        List<DeliveryRoleExpectation> existing = expectationRepository.findByRoleIdAndStatus(role.getId());
        
        if (!existing.isEmpty()) {
            throw new DuplicateRoleExpectationException(
                    "Role expectations already exist for role: " + request.getRoleName() + 
                    ". Use PUT endpoint to update instead.");
        }

        List<DeliveryRoleExpectation> expectations = new ArrayList<>();

        for (DeliveryRoleExpectationRequest.SkillExpectation skillDto : request.getSkills()) {
            saveSkillLevel(role, skillDto, expectations);
            
            if (skillDto.getSubSkills() != null && !skillDto.getSubSkills().isEmpty()) {
                for (DeliveryRoleExpectationRequest.SubSkillExpectation subDto : skillDto.getSubSkills()) {
                    saveSubSkillLevel(role, skillDto, subDto, expectations);
                }
            }
        }

        expectationRepository.saveAll(expectations);

        return groupExpectationsBySkill(expectations);
    }

    @Override
    @Transactional
    public DeliveryRoleExpectationResponse updateRoleExpectations(UUID roleId, DeliveryRoleExpectationRequest request) {

        validateDeliveryRoleRequest(request);

        Role role = getAndValidateRole(roleId);
        List<DeliveryRoleExpectation> existing = expectationRepository.findByRoleIdAndStatus(role.getId());
        
        if (existing.isEmpty()) {
            throw new SkillValidationException(
                    "No role expectations found for roleId: " + roleId + 
                    ". Use POST endpoint to create instead.");
        }

        // Delete existing expectations
        expectationRepository.deleteByRoleId(role.getId());

        List<DeliveryRoleExpectation> expectations = new ArrayList<>();

        for (DeliveryRoleExpectationRequest.SkillExpectation skillDto : request.getSkills()) {
            saveSkillLevel(role, skillDto, expectations);
            
            if (skillDto.getSubSkills() != null && !skillDto.getSubSkills().isEmpty()) {
                for (DeliveryRoleExpectationRequest.SubSkillExpectation subDto : skillDto.getSubSkills()) {
                    saveSubSkillLevel(role, skillDto, subDto, expectations);
                }
            }
        }

        expectationRepository.saveAll(expectations);

        return groupExpectationsBySkill(expectations);
    }

    @Override
    public DeliveryRoleExpectationResponse getRoleExpectations(String roleName) {

        List<DeliveryRoleExpectation> expectations = expectationRepository.findByRoleNameAndStatus(roleName);

        if (expectations.isEmpty()) {
            return new DeliveryRoleExpectationResponse();
        }

        return groupExpectationsBySkill(expectations);
    }

    @Override
    public List<DeliveryRoleExpectationResponse> getAllRoleExpectations() {

        List<DeliveryRoleExpectation> allExpectations = expectationRepository.findAllActive();

        Map<String, List<DeliveryRoleExpectation>> groupedByRole = allExpectations.stream()
                .collect(Collectors.groupingBy(e -> e.getRole().getRoleName()));

        return groupedByRole.entrySet().stream()
                .map(entry -> groupExpectationsBySkill(entry.getValue()))
                .sorted(Comparator.comparing(DeliveryRoleExpectationResponse::getRole))
                .collect(Collectors.toList());
    }

    @Override
    public RoleListResponse getAvailableRoles() {

        List<String> roles = expectationRepository.findDistinctRoleNames();

        RoleListResponse response = new RoleListResponse();
        response.setRoles(roles);

        return response;
    }

    @Override
    @Transactional
    public void deleteRoleExpectations(String roleName) {

        List<DeliveryRoleExpectation> expectations = expectationRepository.findByRoleNameAndStatus(roleName);
        
        if (expectations.isEmpty()) {
            return;
        }

        // Soft delete - set status to INACTIVE
        expectations.forEach(expectation -> expectation.setStatus("INACTIVE"));
        expectationRepository.saveAll(expectations);
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
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new SkillValidationException("Role not found with ID: " + roleId));
    }

    private Role getOrCreateRoleByName(String roleName) {
        Optional<Role> existingRole = roleRepository.findByRoleName(roleName);
        
        if (existingRole.isPresent()) {
            return existingRole.get();
        } else {
            Role newRole = Role.builder()
                    .roleName(roleName)
                    .description("Role created automatically via expectations")
                    .status("ACTIVE")
                    .build();
            return roleRepository.save(newRole);
        }
    }

    private void validateDeliveryRoleRequest(DeliveryRoleExpectationRequest request) {
        if (request.getRoleName() == null || request.getRoleName().trim().isEmpty()) {
            throw new SkillValidationException("Role name cannot be empty");
        }

        if (request.getSkills() == null || request.getSkills().isEmpty()) {
            throw new SkillValidationException("Skills list cannot be empty");
        }

        // Check for duplicate skill IDs in request
        Set<UUID> skillIds = new HashSet<>();
        for (DeliveryRoleExpectationRequest.SkillExpectation skill : request.getSkills()) {
            if (!skillIds.add(skill.getSkillId())) {
                throw new DuplicateRoleExpectationException(
                        "Duplicate skill ID found in request: " + skill.getSkillId());
            }

            // Check for duplicate subskill IDs within each skill
            if (skill.getSubSkills() != null) {
                Set<UUID> subSkillIds = new HashSet<>();
                for (DeliveryRoleExpectationRequest.SubSkillExpectation sub : skill.getSubSkills()) {
                    if (!subSkillIds.add(sub.getSubSkillId())) {
                        throw new DuplicateRoleExpectationException(
                                "Duplicate subskill ID found in skill " + skill.getSkillId() + ": " + sub.getSubSkillId());
                    }
                }
            }
        }
    }

    private void saveSkillLevel(Role role, DeliveryRoleExpectationRequest.SkillExpectation skillDto, 
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

    private void saveSubSkillLevel(Role role, DeliveryRoleExpectationRequest.SkillExpectation skillDto,
                                  DeliveryRoleExpectationRequest.SubSkillExpectation subDto,
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

        response.setRole(expectations.get(0).getRole().getRoleName());
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

        List<DeliveryRoleExpectation> expectations = expectationRepository.findByRoleNameAndStatus(roleName);

        if (expectations.isEmpty()) {
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

        List<DeliveryRoleExpectation> roleExpectations = expectationRepository.findByRoleNameAndStatus(roleName);
        
        // Get all mandatory skill requirements for this role
        List<UUID> mandatorySkillIds = roleExpectations.stream()
                .filter(expectation -> Boolean.TRUE.equals(expectation.getMandatoryFlag()))
                .map(expectation -> expectation.getSkill().getId())
                .collect(Collectors.toList());

        // Check if resource has all mandatory skills
        boolean hasAllMandatorySkills = resourceSkillIds.containsAll(mandatorySkillIds);

        return hasAllMandatorySkills;
    }
}
