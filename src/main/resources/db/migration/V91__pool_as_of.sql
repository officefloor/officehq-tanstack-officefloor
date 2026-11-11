-- The sheet's today when a pool, and when a pool member, was added (null: from the beginning of
-- time), so the pools can be read as of a date.
ALTER TABLE pool ADD COLUMN added_on DATE;
ALTER TABLE pool_member ADD COLUMN added_on DATE;

-- Every pot set is recorded in the settings history (key pot, no from-date, '' for none).
INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)
    SELECT id, 'pot', NULL, CAST(pot AS VARCHAR), DATE '0001-01-01' FROM sheet WHERE pot IS NOT NULL;
