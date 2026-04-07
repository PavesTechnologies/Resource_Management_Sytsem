-- Add role-off columns to resource_allocation table
-- Migration script for adding role-off functionality

ALTER TABLE resource_allocation 
ADD COLUMN role_off_date DATE NULL COMMENT 'Date when resource is role-off from this allocation';

ALTER TABLE resource_allocation 
ADD COLUMN role_off_reason VARCHAR(50) NULL COMMENT 'Reason for role-off (enum values)';

-- Add index for role_off_date for better query performance
CREATE INDEX idx_resource_allocation_role_off_date ON resource_allocation(role_off_date);
