package com.cdc.locking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for distributed scheduler locks.
 * Provides database-level locking operations for multi-instance safety.
 */
@Repository
public interface DistributedSchedulerLockRepository extends JpaRepository<DistributedSchedulerLock, String> {

    /**
     * Find expired locks.
     */
    @Query("SELECT l FROM DistributedSchedulerLock l WHERE l.expiresAt < :now")
    List<DistributedSchedulerLock> findExpiredLocks(@Param("now") LocalDateTime now);

    /**
     * Delete expired locks.
     */
    @Modifying
    @Query("DELETE FROM DistributedSchedulerLock l WHERE l.expiresAt < :now")
    int deleteExpiredLocks(@Param("now") LocalDateTime now);

    /**
     * Find locks by instance ID.
     */
    List<DistributedSchedulerLock> findByInstanceId(String instanceId);

    /**
     * Count active locks for instance.
     */
    @Query("SELECT COUNT(l) FROM DistributedSchedulerLock l WHERE l.instanceId = :instanceId AND l.expiresAt > :now")
    long countActiveLocksForInstance(@Param("instanceId") String instanceId, @Param("now") LocalDateTime now);
}
