package com.cdc.failure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CdcFailureRepository
        extends JpaRepository<CdcFailure, Long> {

    List<CdcFailure> findByStatusAndNextRetryAtBefore(
            String status,
            LocalDateTime time
    );
}
