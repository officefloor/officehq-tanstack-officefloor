-- A rate can carry a from-date: a grade may have several rates, and a claim uses the one whose
-- from-date is the latest on or before the claim's date. A rate with no from-date (null) applies
-- from the beginning of time. Setting a rate for a grade and from-date that already exist replaces
-- it; `from_key` is the from-date as compared for that, with null as the earliest date.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
ALTER TABLE rate ADD COLUMN from_key DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01'));
CREATE UNIQUE INDEX rate_sheet_grade_from ON rate (sheet_id, grade, from_key);
