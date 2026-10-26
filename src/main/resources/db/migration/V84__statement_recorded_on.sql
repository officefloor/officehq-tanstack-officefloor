-- The sheet's today when a statement was taken (null for one taken before this was kept: its taken_on). A statement
-- taken as of an earlier date has that date as its taken_on, but was taken on this one; the statements are listed by it.
ALTER TABLE statement ADD COLUMN recorded_on DATE;
ALTER TABLE hist.statement ADD COLUMN recorded_on DATE;
ALTER TABLE asof.statement ADD COLUMN recorded_on DATE;
