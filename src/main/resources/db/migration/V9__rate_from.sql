-- A rate may carry a from-date: a grade may have several rates, one per from-date, and a claim uses
-- the one with the latest from-date on or before its date. A null from-date applies from the
-- beginning of time. At most one rate per sheet, grade and from-date (null included); the store
-- keeps that by replacing the row it already has.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade);
