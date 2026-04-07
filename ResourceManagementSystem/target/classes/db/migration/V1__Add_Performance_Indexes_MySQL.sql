-- =====================================================
-- Performance Optimization Indexes for Skill Gap Matching
-- =====================================================
-- Migration: V1__Add_Performance_Indexes_MySQL.sql
-- Purpose: Add indexes to optimize skill gap matching queries
-- Created: 2026-02-23
-- Environment: Production-safe with idempotent checks (MySQL)

-- =====================================================
-- Index on resource_skill.proficiency_id
-- Purpose: Optimize proficiency level lookups for resource skills
-- =====================================================
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'resource_skill' 
    AND INDEX_NAME = 'idx_resource_skill_proficiency_id'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_resource_skill_proficiency_id ON resource_skill(proficiency_id)',
    'SELECT ''Index idx_resource_skill_proficiency_id already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- Index on resource_subskill.proficiency_id
-- Purpose: Optimize proficiency level lookups for resource subskills
-- =====================================================
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'resource_subskill' 
    AND INDEX_NAME = 'idx_resource_subskill_proficiency_id'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_resource_subskill_proficiency_id ON resource_subskill(proficiency_id)',
    'SELECT ''Index idx_resource_subskill_proficiency_id already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- Index on delivery_role_expectation.skill_id
-- Purpose: Optimize role expectation queries by skill
-- =====================================================
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'delivery_role_expectation' 
    AND INDEX_NAME = 'idx_delivery_role_expectation_skill_id'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_delivery_role_expectation_skill_id ON delivery_role_expectation(skill_id)',
    'SELECT ''Index idx_delivery_role_expectation_skill_id already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- Index on delivery_role_expectation.sub_skill_id
-- Purpose: Optimize role expectation queries by subskill
-- =====================================================
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'delivery_role_expectation' 
    AND INDEX_NAME = 'idx_delivery_role_expectation_sub_skill_id'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_delivery_role_expectation_sub_skill_id ON delivery_role_expectation(sub_skill_id)',
    'SELECT ''Index idx_delivery_role_expectation_sub_skill_id already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- Additional Performance Indexes
-- Purpose: Further optimize common query patterns
-- =====================================================

-- Composite index for resource skill lookups (resource_id + active_flag)
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'resource_skill' 
    AND INDEX_NAME = 'idx_resource_skill_resource_id_active_flag'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_resource_skill_resource_id_active_flag ON resource_skill(resource_id, active_flag)',
    'SELECT ''Index idx_resource_skill_resource_id_active_flag already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Composite index for resource subskill lookups (resource_id + active_flag)
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'resource_subskill' 
    AND INDEX_NAME = 'idx_resource_subskill_resource_id_active_flag'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_resource_subskill_resource_id_active_flag ON resource_subskill(resource_id, active_flag)',
    'SELECT ''Index idx_resource_subskill_resource_id_active_flag already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Composite index for role expectations (role_name + status)
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'delivery_role_expectation' 
    AND INDEX_NAME = 'idx_delivery_role_expectation_role_name_status'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_delivery_role_expectation_role_name_status ON delivery_role_expectation(role_name, status)',
    'SELECT ''Index idx_delivery_role_expectation_role_name_status already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- Index Verification Query
-- Purpose: Verify all indexes were created successfully
-- =====================================================
SELECT 
    TABLE_NAME as tablename,
    INDEX_NAME as indexname,
    COLUMN_NAME as columnname,
    INDEX_TYPE as indextype
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('resource_skill', 'resource_subskill', 'delivery_role_expectation')
    AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- =====================================================
-- Migration Complete
-- =====================================================
SELECT 'Performance indexes migration completed successfully' as status;
