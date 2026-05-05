-- Role Naming Convention Migration Script
-- Updates old role names to new naming convention
-- Migration Version: 2
-- Date: 2026-05-04

-- Update user roles table (assuming standard user_roles table structure)
-- Adjust table/column names based on your actual database schema

UPDATE user_roles 
SET role_name = 'Resource_Manager' 
WHERE role_name IN ('RESOURCE-MANAGER', 'RESOURCE_MANAGER');

UPDATE user_roles 
SET role_name = 'Admin' 
WHERE role_name = 'ADMIN';

UPDATE user_roles 
SET role_name = 'Project_Manager' 
WHERE role_name IN ('PROJECT-MANAGER', 'PROJECT_MANAGER');

UPDATE user_roles 
SET role_name = 'Delivery_Manager' 
WHERE role_name IN ('DELIVERY-MANAGER', 'DELIVERY_MANAGER', 'DELIVERY_LEAD');

-- Update role_off_events table - role_initiated_by column
UPDATE role_off_events 
SET role_initiated_by = 'Resource_Manager' 
WHERE role_initiated_by = 'RESOURCE-MANAGER';

UPDATE role_off_events 
SET role_initiated_by = 'Admin' 
WHERE role_initiated_by = 'ADMIN';

UPDATE role_off_events 
SET role_initiated_by = 'Project_Manager' 
WHERE role_initiated_by = 'PROJECT-MANAGER';

UPDATE role_off_events 
SET role_initiated_by = 'Delivery_Manager' 
WHERE role_initiated_by = 'DELIVERY-MANAGER';

-- Update role_off_events table - rejected_by column
UPDATE role_off_events 
SET rejected_by = 'Resource_Manager' 
WHERE rejected_by = 'RESOURCE-MANAGER';

UPDATE role_off_events 
SET rejected_by = 'Admin' 
WHERE rejected_by = 'ADMIN';

UPDATE role_off_events 
SET rejected_by = 'Project_Manager' 
WHERE rejected_by = 'PROJECT-MANAGER';

UPDATE role_off_events 
SET rejected_by = 'Delivery_Manager' 
WHERE rejected_by = 'DELIVERY-MANAGER';

-- Update any audit logs or history tables that might contain role information
-- Adjust table/column names based on your actual schema

UPDATE audit_logs 
SET user_role = 'Resource_Manager' 
WHERE user_role = 'RESOURCE-MANAGER';

UPDATE audit_logs 
SET user_role = 'Admin' 
WHERE user_role = 'ADMIN';

UPDATE audit_logs 
SET user_role = 'Project_Manager' 
WHERE user_role = 'PROJECT-MANAGER';

UPDATE audit_logs 
SET user_role = 'Delivery_Manager' 
WHERE user_role = 'DELIVERY-MANAGER';

-- Update any other tables that might store role information
-- This is a template - adjust based on your actual database schema

-- Example for custom user tables:
-- UPDATE users 
-- SET role = 'Resource_Manager' 
-- WHERE role = 'RESOURCE-MANAGER';

-- UPDATE users 
-- SET role = 'Admin' 
-- WHERE role = 'ADMIN';

-- UPDATE users 
-- SET role = 'Project_Manager' 
-- WHERE role = 'PROJECT-MANAGER';

-- UPDATE users 
-- SET role = 'Delivery_Manager' 
-- WHERE role = 'DELIVERY-MANAGER';

-- Commit the changes
COMMIT;

-- Verification queries to check the migration results
SELECT 'Migration completed successfully. Verification:' as status;

-- Check updated role counts
SELECT 
    role_name, 
    COUNT(*) as count 
FROM user_roles 
WHERE role_name IN ('Resource_Manager', 'Admin', 'Project_Manager', 'Delivery_Manager')
GROUP BY role_name;

-- Check for any remaining old role names
SELECT 
    'Remaining old role names found:' as warning,
    role_name, 
    COUNT(*) as count 
FROM user_roles 
WHERE role_name IN ('RESOURCE-MANAGER', 'ADMIN', 'PROJECT-MANAGER', 'DELIVERY-MANAGER', 'RESOURCE_MANAGER', 'PROJECT_MANAGER', 'DELIVERY_MANAGER', 'DELIVERY_LEAD')
GROUP BY role_name;
