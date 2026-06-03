package com.repo.skill_repo;

import com.entity.skill_entities.SkillRequest;
import com.entity_enums.skill_enums.SkillRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SkillRequestRepository extends JpaRepository<SkillRequest, UUID> {

    List<SkillRequest> findByRequestStatusOrderByRequestedAtDesc(SkillRequestStatus status);

    @Query("SELECT sr FROM SkillRequest sr " +
           "LEFT JOIN FETCH sr.proficiency " +
           "ORDER BY sr.requestedAt DESC")
    List<SkillRequest> findAllWithProficiency();

    @Query("SELECT sr FROM SkillRequest sr " +
           "LEFT JOIN FETCH sr.proficiency " +
           "WHERE sr.requestStatus = :status " +
           "ORDER BY sr.requestedAt DESC")
    List<SkillRequest> findByStatusWithProficiency(@Param("status") SkillRequestStatus status);

    List<SkillRequest> findByResourceIdOrderByRequestedAtDesc(String resourceId);

    @Query("SELECT sr FROM SkillRequest sr " +
           "LEFT JOIN FETCH sr.proficiency " +
           "WHERE sr.resourceId = :resourceId AND sr.id IN :requestIds " +
           "ORDER BY sr.requestedAt DESC")
    List<SkillRequest> findByResourceIdAndIdIn(
            @Param("resourceId") String resourceId,
            @Param("requestIds") List<UUID> requestIds);
}
