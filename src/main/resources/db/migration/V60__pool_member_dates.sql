-- A pool membership may carry a from-date and a to-date (either optional; the app refuses a to-date before the
-- from-date). A member whose dates do not include the sheet's today is left out of the pool entirely: not in its total
-- share, not in its balance, and no cut.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
-- The sheet's snapshots, and their layout as of a date, carry them too.
ALTER TABLE hist.pool_member ADD COLUMN from_date DATE;
ALTER TABLE hist.pool_member ADD COLUMN to_date DATE;
ALTER TABLE asof.pool_member ADD COLUMN from_date DATE;
ALTER TABLE asof.pool_member ADD COLUMN to_date DATE;
