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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEventLogRepository extends JpaRepository<LedgerEventLog, Long> {

    Optional<LedgerEventLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    boolean existsByEventHash(String eventHash);

    List<LedgerEventLog> findByResourceId(String resourceId);

    List<LedgerEventLog> findByResourceIdAndStatus(String resourceId, EventStatus status);

    List<LedgerEventLog> findByStatus(EventStatus status);

    List<LedgerEventLog> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries);

    List<LedgerEventLog> findByConnectorNameAndStatusIn(String connectorName, List<EventStatus> statuses);

    List<LedgerEventLog> findByProcessedFlagFalse();

    @Query("SELECT lel FROM LedgerEventLog lel WHERE lel.status = :status AND lel.retryCount < :maxRetries AND (lel.nextRetryAt IS NULL OR lel.nextRetryAt <= :since)")
    List<LedgerEventLog> findRetryableEvents(@Param("status") EventStatus status,
                                             @Param("maxRetries") int maxRetries,
                                             @Param("since") LocalDateTime since);

    @Query("""
        SELECT lel FROM LedgerEventLog lel
        WHERE lel.connectorName IS NOT NULL
          AND lel.status IN :statuses
          AND (lel.nextRetryAt IS NULL OR lel.nextRetryAt <= :now)
        ORDER BY lel.createdAt ASC
        """)
    List<LedgerEventLog> findReadyCdcEvents(@Param("statuses") List<EventStatus> statuses,
                                            @Param("now") LocalDateTime now,
                                            Pageable pageable);

    @Query("""
        SELECT lel FROM LedgerEventLog lel
        WHERE lel.connectorName IS NOT NULL
          AND lel.status = :status
          AND lel.processingStartedAt IS NOT NULL
          AND lel.processingCompletedAt IS NULL
          AND lel.processingStartedAt < :timeoutThreshold
        """)
    List<LedgerEventLog> findStalledCdcEvents(@Param("status") EventStatus status,
                                              @Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    @Query("SELECT COUNT(lel) FROM LedgerEventLog lel WHERE lel.resourceId = :resourceId AND lel.status = :status AND lel.createdAt BETWEEN :startTime AND :endTime")
    Long countEventsByResourceAndStatusInTimeRange(@Param("resourceId") String resourceId,
                                                   @Param("status") EventStatus status,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    @Query("SELECT lel FROM LedgerEventLog lel WHERE lel.processingStartedAt IS NOT NULL AND lel.processingCompletedAt IS NULL AND lel.processingStartedAt < :timeoutThreshold")
    List<LedgerEventLog> findStalledProcessingEvents(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    @Modifying
    @Transactional
    @Query("UPDATE LedgerEventLog lel SET lel.status = :newStatus, lel.errorMessage = :errorMessage, lel.retryCount = lel.retryCount + 1, lel.updatedAt = :updatedAt WHERE lel.eventId = :eventId")
    int markEventAsFailed(@Param("eventId") String eventId,
                          @Param("newStatus") EventStatus newStatus,
                          @Param("errorMessage") String errorMessage,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE LedgerEventLog lel SET lel.status = :newStatus, lel.processingCompletedAt = :completedAt, lel.updatedAt = :updatedAt WHERE lel.eventId = :eventId")
    int markEventAsCompleted(@Param("eventId") String eventId,
                             @Param("newStatus") EventStatus newStatus,
                             @Param("completedAt") LocalDateTime completedAt,
                             @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE LedgerEventLog lel SET lel.processingStartedAt = :startedAt, lel.status = :status, lel.updatedAt = :updatedAt WHERE lel.eventId = :eventId")
    int markEventAsProcessing(@Param("eventId") String eventId,
                              @Param("startedAt") LocalDateTime startedAt,
                              @Param("status") EventStatus status,
                              @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :claimedStatus,
            lel.claimOwner = :claimOwner,
            lel.claimedAt = :claimedAt,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.status IN :claimableStatuses
          AND (lel.nextRetryAt IS NULL OR lel.nextRetryAt <= :updatedAt)
        """)
    int claimEvent(@Param("id") Long id,
                   @Param("claimableStatuses") List<EventStatus> claimableStatuses,
                   @Param("claimedStatus") EventStatus claimedStatus,
                   @Param("claimOwner") String claimOwner,
                   @Param("claimedAt") Instant claimedAt,
                   @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :processingStatus,
            lel.processingStartedAt = :startedAt,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.status = :claimedStatus
          AND lel.claimOwner = :claimOwner
        """)
    int markClaimedEventAsProcessing(@Param("id") Long id,
                                     @Param("claimedStatus") EventStatus claimedStatus,
                                     @Param("processingStatus") EventStatus processingStatus,
                                     @Param("claimOwner") String claimOwner,
                                     @Param("startedAt") LocalDateTime startedAt,
                                     @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :successStatus,
            lel.processedFlag = true,
            lel.errorMessage = null,
            lel.processingCompletedAt = :completedAt,
            lel.claimOwner = null,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.claimOwner = :claimOwner
        """)
    int markCdcEventAsSuccess(@Param("id") Long id,
                              @Param("claimOwner") String claimOwner,
                              @Param("successStatus") EventStatus successStatus,
                              @Param("completedAt") LocalDateTime completedAt,
                              @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :retryStatus,
            lel.errorMessage = :errorMessage,
            lel.retryCount = :retryCount,
            lel.nextRetryAt = :nextRetryAt,
            lel.lastErrorAt = :lastErrorAt,
            lel.claimOwner = null,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.claimOwner = :claimOwner
        """)
    int rescheduleCdcEvent(@Param("id") Long id,
                           @Param("claimOwner") String claimOwner,
                           @Param("retryStatus") EventStatus retryStatus,
                           @Param("errorMessage") String errorMessage,
                           @Param("retryCount") int retryCount,
                           @Param("nextRetryAt") LocalDateTime nextRetryAt,
                           @Param("lastErrorAt") Instant lastErrorAt,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :deadLetterStatus,
            lel.errorMessage = :errorMessage,
            lel.lastErrorAt = :lastErrorAt,
            lel.claimOwner = null,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.claimOwner = :claimOwner
        """)
    int markCdcEventAsDeadLetter(@Param("id") Long id,
                                 @Param("claimOwner") String claimOwner,
                                 @Param("deadLetterStatus") EventStatus deadLetterStatus,
                                 @Param("errorMessage") String errorMessage,
                                 @Param("lastErrorAt") Instant lastErrorAt,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :waitingStatus,
            lel.errorMessage = :errorMessage,
            lel.nextRetryAt = :nextRetryAt,
            lel.lastErrorAt = :lastErrorAt,
            lel.processingCompletedAt = :completedAt,
            lel.claimOwner = null,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.claimOwner = :claimOwner
        """)
    int markCdcEventAsWaiting(@Param("id") Long id,
                              @Param("claimOwner") String claimOwner,
                              @Param("waitingStatus") EventStatus waitingStatus,
                              @Param("errorMessage") String errorMessage,
                              @Param("nextRetryAt") LocalDateTime nextRetryAt,
                              @Param("lastErrorAt") Instant lastErrorAt,
                              @Param("completedAt") LocalDateTime completedAt,
                              @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :cancelledStatus,
            lel.processedFlag = true,
            lel.errorMessage = :errorMessage,
            lel.lastErrorAt = :lastErrorAt,
            lel.nextRetryAt = null,
            lel.processingCompletedAt = :completedAt,
            lel.claimOwner = null,
            lel.updatedAt = :updatedAt
        WHERE lel.id = :id
          AND lel.claimOwner = :claimOwner
        """)
    int markCdcEventAsCancelled(@Param("id") Long id,
                                @Param("claimOwner") String claimOwner,
                                @Param("cancelledStatus") EventStatus cancelledStatus,
                                @Param("errorMessage") String errorMessage,
                                @Param("lastErrorAt") Instant lastErrorAt,
                                @Param("completedAt") LocalDateTime completedAt,
                                @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :newStatus,
            lel.nextRetryAt = :nextRetryAt,
            lel.updatedAt = :updatedAt
        WHERE lel.eventId = :eventId
        """)
    int updateStatusAndRetryTime(@Param("eventId") String eventId,
                                 @Param("newStatus") EventStatus newStatus,
                                 @Param("nextRetryAt") LocalDateTime nextRetryAt,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT lel FROM LedgerEventLog lel WHERE lel.createdAt < :cutoffDate")
    Page<LedgerEventLog> findOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM LedgerEventLog lel WHERE lel.createdAt < :cutoffDate AND lel.status = :status")
    int deleteOldCompletedEvents(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("status") EventStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM LedgerEventLog lel WHERE lel.createdAt < :cutoffDate")
    int deleteOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :retryStatus,
            lel.errorMessage = null,
            lel.nextRetryAt = :nextRetryAt,
            lel.updatedAt = :updatedAt
        WHERE lel.connectorName = :connectorName
          AND lel.sourceTable = :sourceTable
          AND lel.status = :waitingStatus
          AND lel.entityId IN :entityIds
        """)
    int releaseWaitingDependencyEvents(@Param("connectorName") String connectorName,
                                       @Param("sourceTable") String sourceTable,
                                       @Param("waitingStatus") EventStatus waitingStatus,
                                       @Param("retryStatus") EventStatus retryStatus,
                                       @Param("entityIds") List<String> entityIds,
                                       @Param("nextRetryAt") LocalDateTime nextRetryAt,
                                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        UPDATE LedgerEventLog lel
        SET lel.status = :cancelledStatus,
            lel.processedFlag = true,
            lel.errorMessage = :errorMessage,
            lel.nextRetryAt = null,
            lel.processingCompletedAt = :completedAt,
            lel.claimOwner = null,
            lel.updatedAt = :updatedAt
        WHERE lel.connectorName = :connectorName
          AND lel.sourceTable = :sourceTable
          AND lel.status = :waitingStatus
          AND lel.createdAt < :cutoffDate
        """)
    int cancelExpiredWaitingDependencyEvents(@Param("connectorName") String connectorName,
                                             @Param("sourceTable") String sourceTable,
                                             @Param("waitingStatus") EventStatus waitingStatus,
                                             @Param("cancelledStatus") EventStatus cancelledStatus,
                                             @Param("cutoffDate") LocalDateTime cutoffDate,
                                             @Param("errorMessage") String errorMessage,
                                             @Param("completedAt") LocalDateTime completedAt,
                                             @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM LedgerEventLog lel
        WHERE lel.connectorName IS NOT NULL
          AND lel.status = :status
          AND lel.createdAt < :cutoffDate
        """)
    int deleteOldCdcEventsByStatus(@Param("status") EventStatus status,
                                   @Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM LedgerEventLog lel
        WHERE lel.connectorName IS NULL
          AND lel.createdAt < :cutoffDate
        """)
    int deleteOldNonCdcEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT lel.eventType, COUNT(lel) FROM LedgerEventLog lel WHERE lel.createdAt BETWEEN :startTime AND :endTime GROUP BY lel.eventType")
    List<Object[]> getEventStatisticsByType(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT lel.status, COUNT(lel) FROM LedgerEventLog lel WHERE lel.createdAt BETWEEN :startTime AND :endTime GROUP BY lel.status")
    List<Object[]> getEventStatisticsByStatus(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
