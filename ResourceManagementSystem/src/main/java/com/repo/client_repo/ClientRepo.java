package com.repo.client_repo;

import com.entity.client_entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepo extends JpaRepository<Client,Long>, JpaSpecificationExecutor<Client> {
    Optional<Client> findById(Long aLong);

}
