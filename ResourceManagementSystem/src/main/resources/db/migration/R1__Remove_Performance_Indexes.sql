-- =====================================================
-- Rollback Script for Performance Optimization Indexes
-- =====================================================
-- Migration: R1__Remove_Performance_Indexes.sql
-- Purpose: Safely remove performance indexes if needed
-- Created: 2026-02-23
-- Environment: Production-safe with idempotent checks

-- =====================================================
-- Drop Index on resource_skill.proficiency_id
-- =====================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_skill' 
        AND indexname = 'idx_resource_skill_proficiency_id'
    ) THEN
        DROP INDEX IF EXISTS idx_resource_skill_proficiency_id;
        RAISE NOTICE 'Dropped index: idx_resource_skill_proficiency_id';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_resource_skill_proficiency_id';
    END IF;
END $$;

-- =====================================================
-- Drop Index on resource_subskill.proficiency_id
-- =====================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_subskill' 
        AND indexname = 'idx_resource_subskill_proficiency_id'
    ) THEN
        DROP INDEX IF EXISTS idx_resource_subskill_proficiency_id;
        RAISE NOTICE 'Dropped index: idx_resource_subskill_proficiency_id';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_resource_subskill_proficiency_id';
    END IF;
END $$;

-- =====================================================
-- Drop Index on delivery_role_expectation.skill_id
-- =====================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'delivery_role_expectation' 
        AND indexname = 'idx_delivery_role_expectation_skill_id'
    ) THEN
        DROP INDEX IF EXISTS idx_delivery_role_expectation_skill_id;
        RAISE NOTICE 'Dropped index: idx_delivery_role_expectation_skill_id';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_delivery_role_expectation_skill_id';
    END IF;
END $$;

-- =====================================================
-- Drop Index on delivery_role_expectation.sub_skill_id
-- =====================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'delivery_role_expectation' 
        AND indexname = 'idx_delivery_role_expectation_sub_skill_id'
    ) THEN
        DROP INDEX IF EXISTS idx_delivery_role_expectation_sub_skill_id;
        RAISE NOTICE 'Dropped index: idx_delivery_role_expectation_sub_skill_id';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_delivery_role_expectation_sub_skill_id';
    END IF;
END $$;

-- =====================================================
-- Drop Additional Performance Indexes
-- =====================================================

-- Drop composite index for resource skill lookups
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_skill' 
        AND indexname = 'idx_resource_skill_resource_id_active_flag'
    ) THEN
        DROP INDEX IF EXISTS idx_resource_skill_resource_id_active_flag;
        RAISE NOTICE 'Dropped index: idx_resource_skill_resource_id_active_flag';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_resource_skill_resource_id_active_flag';
    END IF;
END $$;

-- Drop composite index for resource subskill lookups
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource_subskill' 
        AND indexname = 'idx_resource_subskill_resource_id_active_flag'
    ) THEN
        DROP INDEX IF EXISTS idx_resource_subskill_resource_id_active_flag;
        RAISE NOTICE 'Dropped index: idx_resource_subskill_resource_id_active_flag';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_resource_subskill_resource_id_active_flag';
    END IF;
END $$;

-- Drop composite index for role expectations
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'delivery_role_expectation' 
        AND indexname = 'idx_delivery_role_expectation_role_name_status'
    ) THEN
        DROP INDEX IF EXISTS idx_delivery_role_expectation_role_name_status;
        RAISE NOTICE 'Dropped index: idx_delivery_role_expectation_role_name_status';
    ELSE
        RAISE NOTICE 'Index does not exist: idx_delivery_role_expectation_role_name_status';
    END IF;
END $$;

-- =====================================================
-- Rollback Complete
-- =====================================================
RAISE NOTICE 'Performance indexes rollback completed successfully';
