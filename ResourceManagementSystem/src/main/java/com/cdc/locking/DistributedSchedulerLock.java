package com.cdc.locking;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ENTERPRISE-GRADE Distributed Scheduler Lock Entity.
 * 
 * Provides database-level distributed locking for CDC schedulers
 * to prevent duplicate execution in multi-instance deployments.
 * 
 * Reused by BOTH PMS and EOS CDC schedulers for consistency.
 */
@Entity
@Table(name = "cdc_scheduler_lock", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"lock_name"}))
@Data
public class DistributedSchedulerLock {

    @Id
    @Column(name = "lock_name", length = 100, nullable = false)
    private String lockName;

    @Column(name = "instance_id", length = 100, nullable = false)
    private String instanceId;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Check if lock is still valid (not expired).
     */
    public boolean isValid() {
        return expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Check if lock is expired.
     */
    public boolean isExpired() {
        return !isValid();
    }

    /**
     * Check if lock belongs to specific instance.
     */
    public boolean belongsTo(String instanceId) {
        return this.instanceId != null && this.instanceId.equals(instanceId);
    }
}
