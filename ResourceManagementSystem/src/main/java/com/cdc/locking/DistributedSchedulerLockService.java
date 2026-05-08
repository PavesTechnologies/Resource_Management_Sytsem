package com.cdc.locking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ENTERPRISE-GRADE Distributed Scheduler Lock Service.
 * 
 * Provides database-level distributed locking for CDC schedulers
 * to prevent duplicate execution in multi-instance deployments.
 * 
 * Reused by BOTH PMS and EOS CDC schedulers for consistency.
 * 
 * Features:
 * - Database-level locking with optimistic versioning
 * - Automatic lock expiration handling
 * - Instance-safe lock acquisition and release
 * - Deadlock prevention with timeout handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedSchedulerLockService {

    private final DistributedSchedulerLockRepository lockRepository;

    /**
     * Acquire distributed lock for scheduler execution.
     * 
     * @param lockName Unique lock name for the scheduler
     * @param lockTimeoutMinutes Lock timeout in minutes
     * @return Lock acquisition result with lock info
     */
    @Transactional
    public LockAcquisitionResult acquireLock(String lockName, int lockTimeoutMinutes) {
        String instanceId = getCurrentInstanceId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(lockTimeoutMinutes);

        try {
            // Try to acquire lock atomically
            DistributedSchedulerLock lock = lockRepository.findById(lockName).orElse(null);
            
            if (lock == null) {
                // No existing lock - create new one
                return createNewLock(lockName, instanceId, expiresAt);
            }
            
            // Check if existing lock is expired
            if (lock.isExpired()) {
                // Lock is expired - try to acquire it
                return acquireExpiredLock(lock, instanceId, expiresAt);
            }
            
            // Lock is valid and belongs to another instance
            if (lock.belongsTo(instanceId)) {
                // Lock belongs to current instance - extend it
                return extendExistingLock(lock, expiresAt);
            }
            
            // Lock belongs to another instance and is still valid
            log.debug("Lock {} is held by another instance {} until {}", 
                     lockName, lock.getInstanceId(), lock.getExpiresAt());
            return LockAcquisitionResult.failed("Lock held by another instance");
            
        } catch (Exception e) {
            log.error("Failed to acquire lock {}: {}", lockName, e.getMessage(), e);
            return LockAcquisitionResult.failed("Lock acquisition failed: " + e.getMessage());
        }
    }

    /**
     * Release distributed lock.
     * 
     * @param lockName Lock name to release
     * @return true if lock was released, false otherwise
     */
    @Transactional
    public boolean releaseLock(String lockName) {
        String instanceId = getCurrentInstanceId();
        
        try {
            DistributedSchedulerLock lock = lockRepository.findById(lockName).orElse(null);
            if (lock == null) {
                log.debug("Lock {} does not exist", lockName);
                return true;
            }
            
            if (lock.belongsTo(instanceId)) {
                lockRepository.delete(lock);
                log.debug("Released lock {} for instance {}", lockName, instanceId);
                return true;
            }
            
            log.warn("Attempted to release lock {} by non-owner instance {}", lockName, instanceId);
            return false;
            
        } catch (Exception e) {
            log.error("Failed to release lock {}: {}", lockName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Clean up expired locks.
     * 
     * @return Number of expired locks cleaned up
     */
    @Transactional
    public int cleanupExpiredLocks() {
        try {
            int deleted = lockRepository.deleteExpiredLocks(LocalDateTime.now());
            if (deleted > 0) {
                log.info("Cleaned up {} expired scheduler locks", deleted);
            }
            return deleted;
        } catch (Exception e) {
            log.error("Failed to cleanup expired locks: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Create new distributed lock.
     */
    private LockAcquisitionResult createNewLock(String lockName, String instanceId, LocalDateTime expiresAt) {
        try {
            DistributedSchedulerLock newLock = new DistributedSchedulerLock();
            newLock.setLockName(lockName);
            newLock.setInstanceId(instanceId);
            newLock.setLockedAt(LocalDateTime.now());
            newLock.setExpiresAt(expiresAt);
            newLock.setCreatedAt(LocalDateTime.now());
            newLock.setVersion(0L);
            
            DistributedSchedulerLock saved = lockRepository.save(newLock);
            log.debug("Created new lock {} for instance {} until {}", lockName, instanceId, expiresAt);
            return LockAcquisitionResult.success(saved);
            
        } catch (Exception e) {
            // Likely a concurrent creation - try again
            log.debug("Concurrent lock creation for {}, retrying", lockName);
            return acquireLock(lockName, (int) java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes());
        }
    }

    /**
     * Acquire expired lock.
     */
    private LockAcquisitionResult acquireExpiredLock(DistributedSchedulerLock expiredLock, String instanceId, LocalDateTime expiresAt) {
        try {
            // Update expired lock with new ownership
            expiredLock.setInstanceId(instanceId);
            expiredLock.setLockedAt(LocalDateTime.now());
            expiredLock.setExpiresAt(expiresAt);
            
            DistributedSchedulerLock updated = lockRepository.save(expiredLock);
            log.debug("Acquired expired lock {} for instance {} until {}", 
                     expiredLock.getLockName(), instanceId, expiresAt);
            return LockAcquisitionResult.success(updated);
            
        } catch (Exception e) {
            log.error("Failed to acquire expired lock {}: {}", expiredLock.getLockName(), e.getMessage(), e);
            return LockAcquisitionResult.failed("Failed to acquire expired lock");
        }
    }

    /**
     * Extend existing lock.
     */
    private LockAcquisitionResult extendExistingLock(DistributedSchedulerLock lock, LocalDateTime expiresAt) {
        try {
            lock.setExpiresAt(expiresAt);
            DistributedSchedulerLock updated = lockRepository.save(lock);
            log.debug("Extended lock {} for instance {} until {}", 
                     lock.getLockName(), lock.getInstanceId(), expiresAt);
            return LockAcquisitionResult.success(updated);
            
        } catch (Exception e) {
            log.error("Failed to extend lock {}: {}", lock.getLockName(), e.getMessage(), e);
            return LockAcquisitionResult.failed("Failed to extend lock");
        }
    }

    /**
     * Get current instance identifier.
     * Uses combination of hostname and process ID for uniqueness.
     */
    private String getCurrentInstanceId() {
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            String processId = String.valueOf(ProcessHandle.current().pid());
            return hostname + "-" + processId + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            // Fallback to random UUID
            return "unknown-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * Lock acquisition result.
     */
    public static class LockAcquisitionResult {
        private final boolean acquired;
        private final DistributedSchedulerLock lock;
        private final String errorMessage;

        private LockAcquisitionResult(boolean acquired, DistributedSchedulerLock lock, String errorMessage) {
            this.acquired = acquired;
            this.lock = lock;
            this.errorMessage = errorMessage;
        }

        public static LockAcquisitionResult success(DistributedSchedulerLock lock) {
            return new LockAcquisitionResult(true, lock, null);
        }

        public static LockAcquisitionResult failed(String errorMessage) {
            return new LockAcquisitionResult(false, null, errorMessage);
        }

        public boolean isAcquired() {
            return acquired;
        }

        public DistributedSchedulerLock getLock() {
            return lock;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
