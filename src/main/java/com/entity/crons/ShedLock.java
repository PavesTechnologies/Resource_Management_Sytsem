package com.entity.crons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA mirror of the ShedLock coordination table.
 *
 * ShedLock's JdbcTemplateLockProvider owns all writes to this table via raw SQL.
 * This entity exists only so that Hibernate (ddl-auto=update) creates the table
 * with the exact schema ShedLock requires.  Never use this entity for reads/writes
 * in application code — use the LockProvider API instead.
 *
 * Required DDL (MySQL/MariaDB):
 *   CREATE TABLE shedlock (
 *     name        VARCHAR(64)  NOT NULL,
 *     lock_until  DATETIME(3)  NOT NULL,
 *     locked_at   DATETIME(3)  NOT NULL,
 *     locked_by   VARCHAR(255) NOT NULL,
 *     PRIMARY KEY (name)
 *   );
 */
@Entity
@Table(name = "shedlock")
public class ShedLock {

    @Id
    @Column(length = 64, nullable = false)
    private String name;

    // DATETIME(3) = millisecond precision; ShedLock uses usingDbTime() which
    // compares lock_until against the DB clock — precision must match exactly.
    @Column(name = "lock_until", columnDefinition = "DATETIME(3)", nullable = false)
    private LocalDateTime lockUntil;

    @Column(name = "locked_at", columnDefinition = "DATETIME(3)", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 255, nullable = false)
    private String lockedBy;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
}
