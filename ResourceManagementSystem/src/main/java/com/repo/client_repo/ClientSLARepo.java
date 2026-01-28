package com.repo.client_repo;

import com.entity.client_entities.ClientSLA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientSLARepo extends JpaRepository<ClientSLA,Long> {
    Optional<ClientSLA> findAllByClient_ClientId(Long aLong);
}
