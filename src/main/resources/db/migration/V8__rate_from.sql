-- A rate can carry a from-date: a grade may have several rates, and a claim uses the one whose
-- from-date is the latest on or before the claim's date. A null from-date applies from the
-- beginning of time. One rate per (sheet, grade, from-date), a null from-date counting as one value.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
