package com.repo;

import com.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Modifying
    @Transactional
    @Query("""
        update Project p
        set p.projectStatus = 'ARCHIVED',
            p.lastSyncedAt = CURRENT_TIMESTAMP
        where p.projectId = :projectId
    """)
    void archiveByProjectId(Long projectId);
}
