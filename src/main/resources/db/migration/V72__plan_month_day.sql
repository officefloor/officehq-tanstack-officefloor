-- A plan may instead run monthly on a day of the month (1 to 31): its dates are that day in every month from the
-- start's month (a day beyond the month's length falls on its last day), dates before the start left out. A monthly
-- plan has a month day and no every N days; any other plan has every N days and no month day.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN month_day INT;
