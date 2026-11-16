-- A plan may instead run monthly on a day of the month (1 to 31): month_day holds it and every_days is then null.
-- A plan with every_days keeps a null month_day.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN month_day INT;
