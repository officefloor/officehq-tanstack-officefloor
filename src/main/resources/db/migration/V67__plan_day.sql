-- A plan may instead run monthly on a day of the month (1 to 31): its dates are that day in every month from the start's
-- month (a day beyond the month's length falling on its last day), dates before the start left out. A monthly plan
-- keeps every_days 0; a plan of every N days keeps day_of_month 0.
ALTER TABLE plan ADD COLUMN day_of_month INT DEFAULT 0 NOT NULL;
