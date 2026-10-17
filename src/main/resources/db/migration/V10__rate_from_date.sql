-- A rate may carry a from-date: it applies from that date on, until the grade's next later from-date.
-- A rate with no from-date applies from the beginning of time. A grade may have several rates, at most
-- one per from-date (and one with none); setting a grade and from-date again replaces that rate.
-- A claim uses the rate whose from-date is the latest on or before its date; none before them all.
DROP INDEX rate_sheet_grade;
ALTER TABLE rate ADD COLUMN from_date DATE;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade);
