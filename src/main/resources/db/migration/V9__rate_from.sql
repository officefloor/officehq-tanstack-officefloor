-- A rate may carry a from-date: a grade may have several rates, one per from-date, and a claim uses the one
-- whose from-date is the latest on or before its date. A null from-date applies from the beginning of time.
-- from_key is the from-date as compared, so there is one rate per sheet, grade and from-date (null included).
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
ALTER TABLE rate ADD COLUMN from_key DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01'));
CREATE UNIQUE INDEX rate_sheet_grade_from ON rate (sheet, grade, from_key);
