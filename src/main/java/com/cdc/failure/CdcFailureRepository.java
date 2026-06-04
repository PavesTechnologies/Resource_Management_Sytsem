package com.cdc.failure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CdcFailureRepository
        extends JpaRepository<CdcFailure, UUID> {

    List<CdcFailure> findByStatusAndNextRetryAtBefore(
            String status,
            LocalDateTime time
    );

    /**
     * Delete old resolved CDC failures.
     * REUSED from LedgerRetryService pattern for consistency.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CdcFailure f WHERE f.status = :status AND f.createdAt < :cutoff")
    int deleteOldFailures(String status, LocalDateTime cutoff);
}
