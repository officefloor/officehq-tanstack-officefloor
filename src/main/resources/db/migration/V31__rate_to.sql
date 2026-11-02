-- A rate can carry a to-date: it applies through that date (null: with no end). A claim dated after
-- the to-date of the rate that would apply to it (the latest from-date on or before the claim's date)
-- has no rate; an earlier rate does not come back.
ALTER TABLE rate ADD COLUMN to_date DATE;
