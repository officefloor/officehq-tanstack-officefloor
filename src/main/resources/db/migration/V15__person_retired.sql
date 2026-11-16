-- A person can be retired from a date. A person with a retirement date counts as retired at once, whatever the date;
-- no claim of theirs can be dated after it. NULL: not retired.
ALTER TABLE person ADD COLUMN retired_from DATE;
