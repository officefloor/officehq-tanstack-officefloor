-- The date a person is retired from; null when they are not retired. A person with a date set is
-- retired at once, whatever the date. No claim of theirs may be dated after it.
ALTER TABLE person ADD COLUMN retired_from DATE;
