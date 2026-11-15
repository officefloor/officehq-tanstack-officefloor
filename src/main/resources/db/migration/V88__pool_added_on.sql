-- The sheet's today when each pool, pool membership, crew and crew membership was added (none: added
-- before it was kept, counting as of every date), so a member's cut can be worked out as it stood at the
-- end of a past period.
ALTER TABLE pool ADD COLUMN added_on DATE;
ALTER TABLE pool_member ADD COLUMN added_on DATE;
ALTER TABLE crew ADD COLUMN added_on DATE;
ALTER TABLE crew_member ADD COLUMN added_on DATE;
