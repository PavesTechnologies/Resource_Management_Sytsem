package com.repo;

import com.entity.Demand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DemandRepository extends JpaRepository<Demand, UUID> {
}
