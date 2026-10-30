-- The sheet's today when a transferred person arrived (null for anyone not transferred in): a transfer can be undone
-- on that day only.
ALTER TABLE person ADD COLUMN arrived_on DATE;
ALTER TABLE person_merged ADD COLUMN arrived_on DATE BEFORE merged_on;
