package com.repo.resource_repo;

import com.entity.resource_entities.Resource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface ResourceRepository extends JpaRepository<Resource, String> {

    Optional<Resource> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Resource> findByEmailIgnoreCase(String email);

    @Query("SELECT DISTINCT r.workingLocation FROM Resource r")
    List<String> findDistinctLocations();

    @Query("SELECT DISTINCT r.designation FROM Resource r")
    List<String> findDistinctDesignations();

    @Query("SELECT MAX(r.experiance) FROM Resource r")
    Long findMaxExperience();
    
    /**
     * Batch query to fetch multiple resources in a single round-trip
     * This prevents N+1 query problems when validating multiple resources
     */
    List<Resource> findAllByResourceIdIn(List<String> resourceIds);

    /**
     * Find Resource by ID with pessimistic locking for multi-instance safety.
     * REUSED from PMS CDC ProjectRepository pattern.
     * 
     * @param resourceId The resource identifier
     * @return Resource with pessimistic lock or empty if not found
     */
    @Query("SELECT r FROM Resource r WHERE r.resourceId = :resourceId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Resource> findByIdWithLock(String resourceId);
}
