-- A pool membership may carry a from-date and a to-date (both optional, the to-date not before the
-- from-date). A member whose dates do not include the sheet's today is left out of the pool's total
-- share, its balance and its cuts.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
