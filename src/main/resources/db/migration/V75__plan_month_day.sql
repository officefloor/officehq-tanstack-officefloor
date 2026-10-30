-- A plan may instead run monthly on a day of the month (1 to 31): then month_day is set and every_days is null.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN month_day INT;
