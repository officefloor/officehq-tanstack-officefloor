-- A pool membership may carry a from-date and a to-date (either may be absent: open on that side; to never before
-- from). A member whose dates do not include the sheet's today is left out of the pool entirely: not in its total
-- share, not in its balance, and no cut.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
