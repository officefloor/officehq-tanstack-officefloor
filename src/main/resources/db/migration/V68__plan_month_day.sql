-- A plan may instead run monthly on a day of the month (1 to 31); every_days is then 0.
ALTER TABLE plan ADD COLUMN month_day INT;
