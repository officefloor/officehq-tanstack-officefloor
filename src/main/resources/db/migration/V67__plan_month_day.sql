-- A plan may instead run monthly on a day of the month (1 to 31): its plan dates are that day in every month
-- from the start's month (a day beyond the month's length falling on its last day), those before the start
-- left out. Exactly one of every_days and month_day is set.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN month_day INT;
