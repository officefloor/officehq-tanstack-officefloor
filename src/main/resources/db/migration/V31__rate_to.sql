-- A rate may carry a to-date (null: no end), inclusive: it applies through that day. A claim dated after the
-- to-date of the rate that would apply to it, with no later rate in force on its date, has no rate (n/a).
ALTER TABLE rate ADD COLUMN to_date DATE;
