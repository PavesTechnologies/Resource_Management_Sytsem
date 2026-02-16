package com.repo.skill_repo;

import com.entity.skill_entities.ProficiencyLevel;
import com.service_imple.skill_service_impl.ProficiencyServiceImp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProficiencyRepo extends JpaRepository<ProficiencyLevel, UUID> {

    ProficiencyLevel save(ProficiencyLevel proficiencyLevel);
}
