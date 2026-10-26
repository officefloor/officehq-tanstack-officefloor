-- The date a person is retired from (null when not retired). A person with one set counts as retired
-- at once, whatever the date; claims dated after it are refused.
ALTER TABLE person ADD COLUMN retired_from DATE;
