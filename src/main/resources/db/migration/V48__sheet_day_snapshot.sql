-- The schema holding a copy of every row kept for the sheet as it stood when its today moved on (see AsOfSchemas), so
-- the whole screen can be read as of an earlier date.
ALTER TABLE sheet_day ADD COLUMN snapshot VARCHAR(64);
