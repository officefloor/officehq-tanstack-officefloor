-- A steward may withdraw a rate: the newest version of a grade and from-date is removed and the one before it (the
-- newest in rate_version) is put back in rate; with none before it the rate is gone. rate_log keeps the withdrawal as
-- a row marked withdrawn (its rate the version withdrawn), so a sheet as of a date undoes it too.
ALTER TABLE rate_log ADD COLUMN withdrawn BOOLEAN DEFAULT FALSE NOT NULL;
