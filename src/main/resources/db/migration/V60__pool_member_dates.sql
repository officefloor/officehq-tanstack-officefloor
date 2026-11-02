-- A pool membership, a person's or a pool's, may carry a from-date and a to-date (each optional, the to-date never
-- before the from-date). A member whose dates do not include the sheet's today is left out of its pool entirely.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
ALTER TABLE pool_sub ADD COLUMN from_date DATE;
ALTER TABLE pool_sub ADD COLUMN to_date DATE;
