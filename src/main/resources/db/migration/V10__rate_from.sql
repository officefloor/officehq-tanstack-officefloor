-- A rate can carry a from-date: a grade may have several rates, and a claim uses the one whose
-- from-date is the latest on or before the claim's date. A rate without one (null) applies from
-- the beginning of time. A grade has at most one rate per from-date (null included); setting it
-- again replaces it.
ALTER TABLE rate ADD COLUMN from_date DATE;
ALTER TABLE rate DROP PRIMARY KEY;
