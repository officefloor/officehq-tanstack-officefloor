-- A plan may instead run monthly on a day of the month (1 to 31): then every_days is empty and day_of_month is set. Its
-- plan dates are that day in every month from the start's month (a day beyond the month's length falls on its last day),
-- dates before the start left out. Exactly one of the two is set.
ALTER TABLE plan ALTER COLUMN every_days SET NULL;
ALTER TABLE plan ADD COLUMN day_of_month INT CHECK (day_of_month BETWEEN 1 AND 31);
ALTER TABLE plan ADD CONSTRAINT plan_every_or_day CHECK ((every_days IS NULL) <> (day_of_month IS NULL));
