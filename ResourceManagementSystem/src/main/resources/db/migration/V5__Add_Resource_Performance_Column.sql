-- =====================================================
-- Add Resource Performance Column
-- =====================================================
-- Migration: V5__Add_Resource_Performance_Column.sql
-- Purpose: Add resource_performance column to resource table
-- Created: 2026-03-30
-- Environment: Production-safe with idempotent checks

-- =====================================================
-- Add resource_performance column to resource table
-- Purpose: Store performance rating/score for each resource
-- =====================================================
DO $$
BEGIN
    -- Check if the column already exists
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'resource' 
        AND column_name = 'resource_performance'
    ) THEN
        ALTER TABLE resource 
        ADD COLUMN resource_performance NUMERIC(3,2) 
        COMMENT 'Resource performance rating/score (0.00-9.99)';
        
        RAISE NOTICE 'Added column: resource.resource_performance';
    ELSE
        RAISE NOTICE 'Column already exists: resource.resource_performance';
    END IF;
END $$;

-- =====================================================
-- Add index for resource_performance for better query performance
-- Purpose: Optimize queries filtering/sorting by performance
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'resource' 
        AND indexname = 'idx_resource_performance'
    ) THEN
        CREATE INDEX idx_resource_performance 
        ON resource(resource_performance);
        
        RAISE NOTICE 'Created index: idx_resource_performance';
    ELSE
        RAISE NOTICE 'Index already exists: idx_resource_performance';
    END IF;
END $$;

-- =====================================================
-- Add check constraint for valid performance range
-- Purpose: Ensure performance values are within expected range
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.check_constraints 
        WHERE constraint_name = 'chk_resource_performance_range'
    ) THEN
        ALTER TABLE resource 
        ADD CONSTRAINT chk_resource_performance_range 
        CHECK (resource_performance IS NULL OR (resource_performance >= 0.00 AND resource_performance <= 9.99));
        
        RAISE NOTICE 'Added constraint: chk_resource_performance_range';
    ELSE
        RAISE NOTICE 'Constraint already exists: chk_resource_performance_range';
    END IF;
END $$;

-- =====================================================
-- Migration Complete
-- =====================================================
RAISE NOTICE 'Resource performance column migration completed successfully';
