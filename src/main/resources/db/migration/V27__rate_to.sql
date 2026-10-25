-- A rate can carry a to-date: it applies through that date (null: with no end). A claim dated after
-- the to-date of the rate that would apply to it, with no later rate in force then, has no rate.
ALTER TABLE rate ADD COLUMN to_date DATE;
