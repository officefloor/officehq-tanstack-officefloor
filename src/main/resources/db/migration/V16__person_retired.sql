-- The date a person is retired from (null: not retired). A person with one set counts as retired at
-- once, whatever the date; their claims dated after it are refused.
ALTER TABLE person ADD COLUMN retired_from DATE;
