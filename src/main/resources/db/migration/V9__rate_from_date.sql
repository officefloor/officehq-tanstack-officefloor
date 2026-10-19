-- A rate can carry a from-date: a grade may have several rates, each in force from its from-date
-- (NULL: from the beginning of time). One rate per sheet, grade and from-date at most; setting one
-- that exists replaces it.
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD COLUMN from_date DATE;
ALTER TABLE rate ADD CONSTRAINT rate_grade_from UNIQUE NULLS NOT DISTINCT (sheet_id, grade, from_date);
