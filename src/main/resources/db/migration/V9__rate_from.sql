-- A rate applies from its from-date: a grade may have several. A rate with no from-date applies from
-- the beginning of time, kept as 0001-01-01 so it can be part of the key. Setting a rate for a grade
-- and from-date that already exist replaces it.
ALTER TABLE rate ADD COLUMN from_date DATE DEFAULT DATE '0001-01-01' NOT NULL;
ALTER TABLE rate DROP PRIMARY KEY;
ALTER TABLE rate ADD PRIMARY KEY (sheet_id, grade, from_date);
