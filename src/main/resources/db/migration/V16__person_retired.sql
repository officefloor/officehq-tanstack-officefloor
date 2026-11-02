-- The date a person is retired from (null while not retired). A person with a date set counts as
-- retired at once, whatever the date; no claim of theirs may be dated after it.
ALTER TABLE person ADD COLUMN retired_from DATE;
