-- A pool membership may carry a from-date and a to-date (each optional; to on or after from). A member whose
-- dates do not include the sheet's today is left out of the pool: not in its total share, nor its balance, no cut.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
