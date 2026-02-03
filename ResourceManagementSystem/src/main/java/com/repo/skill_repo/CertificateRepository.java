package com.repo.skill_repo;

import com.entity.skill_entities.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findBySkillIdAndActiveFlagTrue(UUID skillId);
}
