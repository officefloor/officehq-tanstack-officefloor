-- A pool membership may carry a from-date and a to-date (each optional, inclusive; the to-date never before the
-- from-date). A member whose dates do not include the sheet's today is left out of its pool entirely: not in the
-- total share, not in the balance, no cut.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
