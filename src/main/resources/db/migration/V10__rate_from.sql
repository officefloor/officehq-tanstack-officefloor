-- A rate may carry a from-date: a grade can have several rates, one per from-date (null = from the
-- beginning of time). A claim uses its grade's rate with the latest from-date on or before its date.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade, from_date);
