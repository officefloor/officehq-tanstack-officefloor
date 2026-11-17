-- To read the pools back as they stood at the end of a date (each period's cuts), each pool, pool membership, crew and
-- crew membership keeps the sheet's today when it was added (null: added before this was kept; there from the
-- beginning).
ALTER TABLE pool ADD COLUMN made_on DATE;
ALTER TABLE pool_member ADD COLUMN made_on DATE;
ALTER TABLE crew ADD COLUMN made_on DATE;
ALTER TABLE crew_member ADD COLUMN made_on DATE;
