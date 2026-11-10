-- A rate may carry a from-date: it applies to claims dated on or after it, until a later from-date
-- for the same grade. A null from-date applies from the beginning of time. At most one rate per
-- sheet, grade and from-date (null counted as one value), kept by RateRepository.save.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade);
