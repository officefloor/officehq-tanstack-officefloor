-- A rate can carry a to-date: it applies through that date (none: on and on). A claim dated after the to-date
-- of the rate that would apply to it has no rate (n/a), unless a later rate is in force then.
ALTER TABLE rate ADD COLUMN to_date DATE;
