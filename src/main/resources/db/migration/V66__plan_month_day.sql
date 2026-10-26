-- A plan may instead run monthly on a day of the month (1 to 31): its dates are that day in every month from the
-- start's month (a day beyond the month's length falls on its last day), dates before the start left out. Such a plan
-- has a month_day and no every_days; every other plan has every_days and no month_day.
ALTER TABLE plan ADD COLUMN month_day INT;
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.plan ADD COLUMN month_day INT;
ALTER TABLE hist.plan ALTER COLUMN every_days SET NULL;
ALTER TABLE asof.plan ADD COLUMN month_day INT;
ALTER TABLE asof.plan ALTER COLUMN every_days SET NULL;
