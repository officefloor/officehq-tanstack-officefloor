-- A plan may instead run monthly on a day of the month (1 to 31): that day in every month from the
-- start's month (a day beyond the month's length falls on its last day), dates before the start left
-- out. A plan has either every_days or day_of_month, never both.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN day_of_month INT;
