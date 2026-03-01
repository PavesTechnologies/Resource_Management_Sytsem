-- Fix SLAType enum mismatch: Update NEW_DEMAND to NET_NEW
UPDATE project_sla SET sla_type = 'NET_NEW' WHERE sla_type = 'NEW_DEMAND';
