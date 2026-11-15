-- A pool membership may run from a date and/or to a date (both inclusive; NULL: no bound). A member whose
-- dates do not include the sheet's today is left out of the pool's total share and gets no cut.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
