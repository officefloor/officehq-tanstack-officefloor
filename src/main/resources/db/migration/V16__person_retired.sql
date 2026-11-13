-- A person can be retired from a date: they count as retired at once, whatever the date, and a claim
-- dated after it is refused. Null while the person is not retired.
ALTER TABLE person ADD COLUMN retired_from DATE;
