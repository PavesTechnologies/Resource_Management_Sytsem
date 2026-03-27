package com.service_imple.ledger_service_impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String LEDGER_DISTRIBUTED_LOCK_PREFIX = "ledger-lock:";
    private static final long LEDGER_LOCK_DEFAULT_TIMEOUT_SECONDS = 30;
    private static final long LEDGER_LOCK_RETRY_INTERVAL_MS = 100;
    private static final int LEDGER_LOCK_MAX_RETRIES = 50;

    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        String fullLockKey = LEDGER_DISTRIBUTED_LOCK_PREFIX + lockKey;
        
        try {
            if (acquireLock(fullLockKey)) {
                try {
                    return action.get();
                } finally {
                    releaseLock(fullLockKey);
                }
            } else {
                throw new RuntimeException("Failed to acquire distributed lock: " + lockKey);
            }
        } catch (Exception e) {
            log.error("Error executing with distributed lock {}: {}", lockKey, e.getMessage(), e);
            throw new RuntimeException("Distributed lock operation failed", e);
        }
    }

    public void executeWithLockVoid(String lockKey, Runnable action) {
        String fullLockKey = LEDGER_DISTRIBUTED_LOCK_PREFIX + lockKey;
        
        try {
            if (acquireLock(fullLockKey)) {
                try {
                    action.run();
                } finally {
                    releaseLock(fullLockKey);
                }
            } else {
                throw new RuntimeException("Failed to acquire distributed lock: " + lockKey);
            }
        } catch (Exception e) {
            log.error("Error executing with distributed lock {}: {}", lockKey, e.getMessage(), e);
            throw new RuntimeException("Distributed lock operation failed", e);
        }
    }

    private boolean acquireLock(String lockKey) {
        return acquireLockWithRetry(lockKey, LEDGER_LOCK_DEFAULT_TIMEOUT_SECONDS);
    }

    private boolean acquireLockWithRetry(String lockKey, long timeoutSeconds) {
        String lockValue = Thread.currentThread().getName() + "-" + System.currentTimeMillis();
        
        for (int attempt = 0; attempt < LEDGER_LOCK_MAX_RETRIES; attempt++) {
            try {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, timeoutSeconds, TimeUnit.SECONDS);
                
                if (Boolean.TRUE.equals(acquired)) {
                    log.debug("Acquired distributed lock: {}", lockKey);
                    return true;
                }
                
                if (attempt < LEDGER_LOCK_MAX_RETRIES - 1) {
                    Thread.sleep(LEDGER_LOCK_RETRY_INTERVAL_MS);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted for: {}", lockKey);
                return false;
            } catch (Exception e) {
                log.error("Error acquiring lock {}: {}", lockKey, e.getMessage(), e);
            }
        }
        
        log.warn("Failed to acquire distributed lock after {} attempts: {}", LEDGER_LOCK_MAX_RETRIES, lockKey);
        return false;
    }

    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
            log.debug("Released distributed lock: {}", lockKey);
        } catch (Exception e) {
            log.error("Error releasing lock {}: {}", lockKey, e.getMessage(), e);
        }
    }

    public boolean isLockHeld(String lockKey) {
        String fullLockKey = LEDGER_DISTRIBUTED_LOCK_PREFIX + lockKey;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullLockKey));
        } catch (Exception e) {
            log.error("Error checking lock status for {}: {}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    public boolean tryLock(String lockKey, long timeoutSeconds) {
        String fullLockKey = LEDGER_DISTRIBUTED_LOCK_PREFIX + lockKey;
        return acquireLockWithRetry(fullLockKey, timeoutSeconds);
    }

    public void unlock(String lockKey) {
        String fullLockKey = LEDGER_DISTRIBUTED_LOCK_PREFIX + lockKey;
        releaseLock(fullLockKey);
    }

    public void executeWithLockTimeout(String lockKey, long timeoutSeconds, Runnable action) {
        String fullLockKey = LEDGER_DISTRIBUTED_LOCK_PREFIX + lockKey;
        
        try {
            if (acquireLockWithRetry(fullLockKey, timeoutSeconds)) {
                try {
                    action.run();
                } finally {
                    releaseLock(fullLockKey);
                }
            } else {
                throw new RuntimeException("Failed to acquire distributed lock: " + lockKey);
            }
        } catch (Exception e) {
            log.error("Error executing with distributed lock {}: {}", lockKey, e.getMessage(), e);
            throw new RuntimeException("Distributed lock operation failed", e);
        }
    }

    public String generateLockKey(Long resourceId, java.time.LocalDate date) {
        return "resource-" + resourceId + "-" + date;
    }

    public String generateEventLockKey(String eventId) {
        return "event-" + eventId;
    }

    public String generateBatchLockKey(String batchId) {
        return "batch-" + batchId;
    }
}
