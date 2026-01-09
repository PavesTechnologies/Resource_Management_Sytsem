package com.repo.skill_repo;

import com.entity.skill_entities.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, String> {

    Optional<Certificate> findBySkillIdAndActiveFlagTrue(Long skillId);
}
