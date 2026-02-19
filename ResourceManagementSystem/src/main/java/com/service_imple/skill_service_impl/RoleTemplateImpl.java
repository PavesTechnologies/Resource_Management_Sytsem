package com.service_imple.skill_service_impl;

import com.dto.skill_dto.CreateRoleTemplateRequestDTO;
import com.entity.skill_entities.RoleTemplate;
import com.entity_enums.skill_enums.TemplateStatus;
import com.repo.skill_repo.RoleTemplateRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleTemplateImpl {

    @Autowired
    RoleTemplateRepo roleTemplateRepository;

    @Transactional
    public RoleTemplate createRoleTemplate(CreateRoleTemplateRequestDTO dto) {

        if (dto.getMinExperienceYears() > dto.getMaxExperienceYears()) {
            throw new RuntimeException("Min experience cannot be greater than max experience");
        }

        RoleTemplate template = RoleTemplate.builder()
                .roleName(dto.getRoleName())
                .minExperienceYears(dto.getMinExperienceYears())
                .maxExperienceYears(dto.getMaxExperienceYears())
                .version(1)
                .status(TemplateStatus.DRAFT)
                .activeFlag(true)
                .locked(false)
                .build();

        return roleTemplateRepository.save(template);
    }

}