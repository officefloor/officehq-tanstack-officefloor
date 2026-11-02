-- A rate can carry a from-date: a grade may have several rates, each in force from its from-date
-- (a rate with none applies from the beginning of time). A grade has at most one rate per from-date
-- (setting it again replaces it); the store keeps that, as a null from-date cannot be in a key.
ALTER TABLE rate ADD COLUMN from_date DATE;
ALTER TABLE rate DROP PRIMARY KEY;
CREATE INDEX rate_sheet_grade ON rate (sheet_id, grade, from_date);
