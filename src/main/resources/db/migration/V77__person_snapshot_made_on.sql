-- The sheet's today a snapshot was written on (the new today it moved to), so a snapshot shows as of a date only once
-- it had been written by then. Snapshots written before this keep their day being left.
ALTER TABLE person_snapshot ADD COLUMN made_on DATE;
UPDATE person_snapshot SET made_on = left_on;
