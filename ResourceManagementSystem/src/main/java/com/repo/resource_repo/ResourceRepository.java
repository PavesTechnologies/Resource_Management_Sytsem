package com.repo.resource_repo;

import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByEmail(String email);
    boolean existsByEmail(String email);

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
    List<Resource> findAllByResourceIdIn(List<Long> resourceIds);
}
