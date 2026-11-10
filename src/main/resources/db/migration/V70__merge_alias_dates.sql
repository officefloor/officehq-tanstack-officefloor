-- The sheet's today when an alias was given (null: from the beginning of time).
ALTER TABLE person_alias ADD COLUMN added_on DATE;
-- The sheet's today when a person was merged into another (null: not merged, or not kept): read as
-- of an earlier date, they are still on the list.
ALTER TABLE person ADD COLUMN merged_on DATE;
