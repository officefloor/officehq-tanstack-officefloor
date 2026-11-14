-- A plan may instead run monthly on a day of the month (1 to 31): then month_day is set and every_days is null. Its
-- dates are that day in every month from the start's month (a day beyond the month's length falls on its last day),
-- dates before the start left out.
ALTER TABLE person_plan ALTER COLUMN every_days SET NULL;
ALTER TABLE person_plan ADD COLUMN month_day INT;
