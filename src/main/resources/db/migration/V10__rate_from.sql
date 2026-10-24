-- A rate can carry a from-date: it applies from that date on (a rate with none applies from the beginning
-- of time). A grade may have several rates, one per from-date; a claim uses the one whose from-date is the
-- latest on or before its date.
ALTER TABLE rate ADD COLUMN from_date DATE;
DROP INDEX rate_sheet_grade;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade);
