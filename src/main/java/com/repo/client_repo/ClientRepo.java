package com.repo.client_repo;

import com.entity.client_entities.Client;
import com.entity_enums.centralised_enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepo extends JpaRepository<Client,UUID>, JpaSpecificationExecutor<Client> {
//    Optional<Client> findById(Long aLong);
    List<Client> findByStatus(RecordStatus status);
}
