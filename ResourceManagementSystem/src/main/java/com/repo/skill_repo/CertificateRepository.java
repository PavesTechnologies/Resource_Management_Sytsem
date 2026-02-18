package com.repo.skill_repo;

import com.entity.skill_entities.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findBySkillId(UUID skillId);
}
