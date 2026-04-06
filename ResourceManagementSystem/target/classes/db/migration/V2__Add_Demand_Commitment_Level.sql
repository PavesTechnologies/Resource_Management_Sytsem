-- Add demand_commitment_level column to demand table
ALTER TABLE demand ADD COLUMN demand_commitment_level VARCHAR(20) NOT NULL DEFAULT 'SOFT';

-- Add check constraint to ensure only valid values
ALTER TABLE demand ADD CONSTRAINT chk_demand_commitment_level 
CHECK (demand_commitment_level IN ('SOFT', 'CONFIRMED'));

-- Add index for better query performance on commitment level
CREATE INDEX idx_demand_commitment_level ON demand(demand_commitment_level);

-- Add composite index for common queries (commitment level + status)
CREATE INDEX idx_demand_commitment_status ON demand(demand_commitment_level, demand_status);
