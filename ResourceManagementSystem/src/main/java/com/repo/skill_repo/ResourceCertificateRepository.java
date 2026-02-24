package com.repo.skill_repo;

import com.entity.skill_entities.ResourceCertificate;
import com.dto.CertificationInfoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceCertificateRepository extends JpaRepository<ResourceCertificate, UUID> {
    
    @Query("SELECT rc FROM ResourceCertificate rc WHERE rc.resourceId = :resourceId AND rc.activeFlag = true AND (rc.expiryDate IS NULL OR rc.expiryDate > :currentDate)")
    List<ResourceCertificate> findActiveCertificatesForResource(@Param("resourceId") Long resourceId, @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT rc FROM ResourceCertificate rc WHERE rc.resourceId = :resourceId AND rc.certificateId = :certificateId AND rc.activeFlag = true AND (rc.expiryDate IS NULL OR rc.expiryDate > :currentDate)")
    ResourceCertificate findActiveCertificateForResource(@Param("resourceId") Long resourceId, @Param("certificateId") UUID certificateId, @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT rc.resourceId, s.name, c.providerName, rc.expiryDate FROM ResourceCertificate rc JOIN rc.certificate c LEFT JOIN Skill s ON c.skillId = s.id WHERE rc.resourceId IN :resourceIds AND rc.activeFlag = true AND (rc.expiryDate IS NULL OR rc.expiryDate > :currentDate)")
    List<Object[]> findResourceIdAndCertificateDetails(@Param("resourceIds") List<Long> resourceIds, @Param("currentDate") LocalDate currentDate);
}
