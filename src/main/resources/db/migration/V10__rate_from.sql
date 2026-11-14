-- A rate can carry a from-date: a grade may have several rates, one per from-date, and a claim uses
-- the one whose from-date is the latest on or before its date. No from-date applies from the
-- beginning of time (at most one such rate per grade).
ALTER TABLE rate ADD COLUMN from_date DATE;
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_key DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01'));
CREATE UNIQUE INDEX rate_sheet_grade_from ON rate (sheet_id, grade, from_key);
