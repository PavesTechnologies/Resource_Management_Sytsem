package com.repo.skill_repo;

import com.entity.skill_entities.ProficiencyLevel;
import com.service_imple.skill_service_impl.ProficiencyServiceImp;
import org.springframework.stereotype.Repository;

@Repository
public interface ProficiencyRepo {

    ProficiencyLevel save(ProficiencyLevel proficiencyLevel);
}
