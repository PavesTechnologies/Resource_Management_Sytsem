-- Find allocation ID for resource 1002
SELECT 
    allocation_id,
    resource_id,
    allocation_status,
    allocation_start_date,
    allocation_end_date,
    allocation_percentage,
    created_at
FROM resource_allocation 
WHERE resource_id = 1002 
AND allocation_status = 'ACTIVE'
ORDER BY created_at DESC;

-- If no active allocations, check all allocations for this resource
SELECT 
    allocation_id,
    resource_id,
    allocation_status,
    allocation_start_date,
    allocation_end_date,
    allocation_percentage,
    created_at
FROM resource_allocation 
WHERE resource_id = 1002
ORDER BY created_at DESC;
