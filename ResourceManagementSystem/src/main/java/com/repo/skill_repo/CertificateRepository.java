package com.repo.skill_repo;

import com.entity.skill_entities.Certificate;
import com.entity_enums.skill_enums.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findBySkillId(UUID skillId);

    List<Certificate> findByCertificateType(CertificateType certificateType);

    List<Certificate> findByActiveFlagTrue();
}
