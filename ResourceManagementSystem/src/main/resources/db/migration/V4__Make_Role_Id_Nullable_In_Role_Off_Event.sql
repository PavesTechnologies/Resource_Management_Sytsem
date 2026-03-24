-- Make role_id column nullable in role_off_event table
-- This allows role_id to be null when autoReplacementRequired is false
ALTER TABLE role_off_event MODIFY COLUMN role_id UUID NULL;
