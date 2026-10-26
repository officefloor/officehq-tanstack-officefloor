-- A rate may carry a from-date: a grade may have several rates, one per from-date. A rate with no
-- from-date (null) applies from the beginning of time. At most one rate per grade and from-date.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade);
