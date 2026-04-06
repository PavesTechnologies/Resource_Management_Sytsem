-- =====================================================
-- Performance Optimization Indexes for Skill Gap Matching
-- =====================================================
-- Migration: V1__Add_Performance_Indexes.sql
-- Purpose: Add indexes to optimize skill gap matching queries
-- Created: 2026-02-23
-- Environment: Production-safe with idempotent checks

-- =====================================================
-- Index on resource_skill.proficiency_id
-- Purpose: Optimize proficiency level lookups for resource skills
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_skill' 
        AND indexname = 'idx_resource_skill_proficiency_id'
    ) THEN
        CREATE INDEX idx_resource_skill_proficiency_id 
        ON resource_skill(proficiency_id);
        RAISE NOTICE 'Created index: idx_resource_skill_proficiency_id';
    ELSE
        RAISE NOTICE 'Index already exists: idx_resource_skill_proficiency_id';
    END IF;
END $$;

-- =====================================================
-- Index on resource_subskill.proficiency_id
-- Purpose: Optimize proficiency level lookups for resource subskills
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_subskill' 
        AND indexname = 'idx_resource_subskill_proficiency_id'
    ) THEN
        CREATE INDEX idx_resource_subskill_proficiency_id 
        ON resource_subskill(proficiency_id);
        RAISE NOTICE 'Created index: idx_resource_subskill_proficiency_id';
    ELSE
        RAISE NOTICE 'Index already exists: idx_resource_subskill_proficiency_id';
    END IF;
END $$;

-- =====================================================
-- Index on delivery_role_expectation.skill_id
-- Purpose: Optimize role expectation queries by skill
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'delivery_role_expectation' 
        AND indexname = 'idx_delivery_role_expectation_skill_id'
    ) THEN
        CREATE INDEX idx_delivery_role_expectation_skill_id 
        ON delivery_role_expectation(skill_id);
        RAISE NOTICE 'Created index: idx_delivery_role_expectation_skill_id';
    ELSE
        RAISE NOTICE 'Index already exists: idx_delivery_role_expectation_skill_id';
    END IF;
END $$;

-- =====================================================
-- Index on delivery_role_expectation.sub_skill_id
-- Purpose: Optimize role expectation queries by subskill
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'delivery_role_expectation' 
        AND indexname = 'idx_delivery_role_expectation_sub_skill_id'
    ) THEN
        CREATE INDEX idx_delivery_role_expectation_sub_skill_id 
        ON delivery_role_expectation(sub_skill_id);
        RAISE NOTICE 'Created index: idx_delivery_role_expectation_sub_skill_id';
    ELSE
        RAISE NOTICE 'Index already exists: idx_delivery_role_expectation_sub_skill_id';
    END IF;
END $$;

-- =====================================================
-- Additional Performance Indexes
-- Purpose: Further optimize common query patterns
-- =====================================================

-- Composite index for resource skill lookups (resource_id + active_flag)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_skill' 
        AND indexname = 'idx_resource_skill_resource_id_active_flag'
    ) THEN
        CREATE INDEX idx_resource_skill_resource_id_active_flag 
        ON resource_skill(resource_id, active_flag);
        RAISE NOTICE 'Created index: idx_resource_skill_resource_id_active_flag';
    ELSE
        RAISE NOTICE 'Index already exists: idx_resource_skill_resource_id_active_flag';
    END IF;
END $$;

-- Composite index for resource subskill lookups (resource_id + active_flag)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_subskill' 
        AND indexname = 'idx_resource_subskill_resource_id_active_flag'
    ) THEN
        CREATE INDEX idx_resource_subskill_resource_id_active_flag 
        ON resource_subskill(resource_id, active_flag);
        RAISE NOTICE 'Created index: idx_resource_subskill_resource_id_active_flag';
    ELSE
        RAISE NOTICE 'Index already exists: idx_resource_subskill_resource_id_active_flag';
    END IF;
END $$;

-- Composite index for role expectations (role_name + status)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'delivery_role_expectation' 
        AND indexname = 'idx_delivery_role_expectation_role_name_status'
    ) THEN
        CREATE INDEX idx_delivery_role_expectation_role_name_status 
        ON delivery_role_expectation(role_name, status);
        RAISE NOTICE 'Created index: idx_delivery_role_expectation_role_name_status';
    ELSE
        RAISE NOTICE 'Index already exists: idx_delivery_role_expectation_role_name_status';
    END IF;
END $$;

-- =====================================================
-- Index Verification Query
-- Purpose: Verify all indexes were created successfully
-- =====================================================
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename IN ('resource_skill', 'resource_subskill', 'delivery_role_expectation')
    AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;

-- =====================================================
-- Migration Complete
-- =====================================================
RAISE NOTICE 'Performance indexes migration completed successfully';
