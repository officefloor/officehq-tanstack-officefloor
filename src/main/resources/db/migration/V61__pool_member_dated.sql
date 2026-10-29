-- A pool membership (a person's or a pool's) may run from a from-date to a to-date, both inclusive and each optional (null
-- is open-ended); the to-date is never before the from-date. A member whose dates do not include the sheet's today is left
-- out entirely: not in the pool's total share, not in its balance, and no cut.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
ALTER TABLE pool_nested ADD COLUMN from_date DATE;
ALTER TABLE pool_nested ADD COLUMN to_date DATE;
