-- A person retired from a date (null: not retired). A person with a date set counts as retired at
-- once, whatever the date; no claim dated after it can be added to them.
ALTER TABLE person ADD COLUMN retired_from DATE;
