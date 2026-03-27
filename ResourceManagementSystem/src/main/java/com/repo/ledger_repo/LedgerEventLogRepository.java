package com.repo.ledger_repo;

import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEventLogRepository extends JpaRepository<LedgerEventLog, Long> {

    Optional<LedgerEventLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    boolean existsByEventHash(String eventHash);

    List<LedgerEventLog> findByResourceId(Long resourceId);

    List<LedgerEventLog> findByResourceIdAndStatus(Long resourceId, EventStatus status);

    List<LedgerEventLog> findByStatus(EventStatus status);

    List<LedgerEventLog> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries);

    List<LedgerEventLog> findByProcessedFlagFalse();

    @Query("SELECT lel FROM LedgerEventLog lel WHERE lel.status = :status AND lel.retryCount < :maxRetries AND lel.createdAt >= :since")
    List<LedgerEventLog> findRetryableEvents(@Param("status") EventStatus status, @Param("maxRetries") int maxRetries, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(lel) FROM LedgerEventLog lel WHERE lel.resourceId = :resourceId AND lel.status = :status AND lel.createdAt BETWEEN :startTime AND :endTime")
    Long countEventsByResourceAndStatusInTimeRange(@Param("resourceId") Long resourceId, @Param("status") EventStatus status, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT lel FROM LedgerEventLog lel WHERE lel.processingStartedAt IS NOT NULL AND lel.processingCompletedAt IS NULL AND lel.processingStartedAt < :timeoutThreshold")
    List<LedgerEventLog> findStalledProcessingEvents(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    @Modifying
    @Query("UPDATE LedgerEventLog lel SET lel.status = :newStatus, lel.errorMessage = :errorMessage, lel.retryCount = lel.retryCount + 1, lel.updatedAt = :updatedAt WHERE lel.eventId = :eventId")
    int markEventAsFailed(@Param("eventId") String eventId, @Param("newStatus") EventStatus newStatus, @Param("errorMessage") String errorMessage, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE LedgerEventLog lel SET lel.status = :newStatus, lel.processingCompletedAt = :completedAt, lel.updatedAt = :updatedAt WHERE lel.eventId = :eventId")
    int markEventAsCompleted(@Param("eventId") String eventId, @Param("newStatus") EventStatus newStatus, @Param("completedAt") LocalDateTime completedAt, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE LedgerEventLog lel SET lel.processingStartedAt = :startedAt, lel.status = :status, lel.updatedAt = :updatedAt WHERE lel.eventId = :eventId")
    int markEventAsProcessing(@Param("eventId") String eventId, @Param("startedAt") LocalDateTime startedAt, @Param("status") EventStatus status, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 🔐 STEP 4: MULTI-INSTANCE SAFETY - Atomic status update with row-level locking
     * 
     * Updates event status to PROCESSING only if currently PENDING
     * Returns number of rows updated (0 or 1)
     * Only one instance can succeed due to WHERE clause
     */
    @Modifying
    @Query("UPDATE LedgerEventLog lel " +
           "SET lel.status = 'PROCESSING', " +
           "    lel.processingStartedAt = :startedAt, " +
           "    lel.updatedAt = :updatedAt " +
           "WHERE lel.eventId = :eventId " +
           "AND lel.status = 'PENDING'")
    int updateEventStatusToProcessing(@Param("eventId") String eventId,
                                   @Param("startedAt") LocalDateTime startedAt,
                                   @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 🔐 STEP 3: STRICT IDEMPOTENCY - Mark event as SUCCESS
     */
    @Modifying
    @Query("UPDATE LedgerEventLog lel " +
           "SET lel.status = 'SUCCESS', " +
           "    lel.processingCompletedAt = :completedAt, " +
           "    lel.updatedAt = :updatedAt " +
           "WHERE lel.eventId = :eventId")
    int updateEventStatusToSuccess(@Param("eventId") String eventId,
                                @Param("completedAt") LocalDateTime completedAt,
                                @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 🔐 STEP 5: RETRY + DLQ - Mark event as FAILED
     */
    @Modifying
    @Query("UPDATE LedgerEventLog lel " +
           "SET lel.status = 'FAILED', " +
           "    lel.errorMessage = :errorMessage, " +
           "    lel.updatedAt = :updatedAt " +
           "WHERE lel.eventId = :eventId")
    int updateEventStatusToFailed(@Param("eventId") String eventId,
                                @Param("errorMessage") String errorMessage,
                                @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT lel FROM LedgerEventLog lel WHERE lel.createdAt < :cutoffDate")
    Page<LedgerEventLog> findOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    @Modifying
    @Query("DELETE FROM LedgerEventLog lel WHERE lel.createdAt < :cutoffDate AND lel.status = :status")
    int deleteOldCompletedEvents(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("status") EventStatus status);

    @Modifying
    @Query("DELETE FROM LedgerEventLog lel WHERE lel.createdAt < :cutoffDate")
    int deleteOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT lel.eventType, COUNT(lel) FROM LedgerEventLog lel WHERE lel.createdAt BETWEEN :startTime AND :endTime GROUP BY lel.eventType")
    List<Object[]> getEventStatisticsByType(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT lel.status, COUNT(lel) FROM LedgerEventLog lel WHERE lel.createdAt BETWEEN :startTime AND :endTime GROUP BY lel.status")
    List<Object[]> getEventStatisticsByStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
