package com.entity.crons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "shedlock")
public class ShedLock {

    @Id
    @Column(length = 64)
    private String name;

    private LocalDateTime lockUntil;
    private LocalDateTime lockedAt;

    @Column(length = 255)
    private String lockedBy;

    // getters + setters
}
