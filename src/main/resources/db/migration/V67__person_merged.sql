-- A person merged into another: the id of the person they were merged into. A merged person is
-- kept for the sheet's history but leaves the list, and no change can be made to them. Null: not merged.
ALTER TABLE person ADD COLUMN merged_into BIGINT;
