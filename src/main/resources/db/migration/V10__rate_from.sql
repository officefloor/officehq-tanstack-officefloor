-- A rate may carry a from-date: a grade may have several rates, one per from-date (and at most one
-- with none, which applies from the beginning of time). A claim uses the rate whose from-date is the
-- latest on or before its date.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade);
