-- A plan may instead run monthly on a day of the month (1 to 31): every_days is then NULL and month_day
-- holds the day; a day beyond a month's length falls on its last day.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN month_day INT;
