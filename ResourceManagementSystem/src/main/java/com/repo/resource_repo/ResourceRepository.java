package com.repo.resource_repo;

import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByEmail(String email);
    boolean existsByEmail(String email);
}
