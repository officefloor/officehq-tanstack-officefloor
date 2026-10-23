-- The sheet's today when each batch happened (null: before this was kept, counting on every date): the last batch as
-- of a date is the last one made on or before it.
ALTER TABLE settle_batch ADD COLUMN made_on DATE;
