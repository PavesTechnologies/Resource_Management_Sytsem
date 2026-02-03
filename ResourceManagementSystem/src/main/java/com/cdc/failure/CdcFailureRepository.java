package com.cdc.failure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CdcFailureRepository
        extends JpaRepository<CdcFailure, UUID> {

    List<CdcFailure> findByStatusAndNextRetryAtBefore(
            String status,
            LocalDateTime time
    );
}
