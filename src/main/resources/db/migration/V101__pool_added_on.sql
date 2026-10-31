-- The sheet's today when each pool, pool membership, crew and crew membership was added, so a pool member's cut can be
-- shown as it stood at the end of a past period. NULL: added before this was kept; it counts as of any date.
ALTER TABLE pool ADD COLUMN added_on DATE;
ALTER TABLE pool_member ADD COLUMN added_on DATE;
ALTER TABLE crew ADD COLUMN added_on DATE;
ALTER TABLE crew_member ADD COLUMN added_on DATE;
