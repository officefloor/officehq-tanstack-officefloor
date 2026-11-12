-- A person retired from a date: no claim dated after it may be added (null while not retired). A
-- person with a date set counts as retired at once, whatever the date.
ALTER TABLE person ADD COLUMN retired_from DATE;
