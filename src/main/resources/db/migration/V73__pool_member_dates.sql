-- A pool membership may be dated: from_date and to_date (each optional, both inclusive; to_date not before
-- from_date). A member whose dates do not include the day judged by is left out of the pool's total and cuts.
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
