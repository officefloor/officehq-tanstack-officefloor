-- A person can be retired from a date. A person with a retirement date set counts as retired at once,
-- whatever the date; no claim dated after it can be added (or edited to such a date).
ALTER TABLE person ADD COLUMN retired_from DATE;
