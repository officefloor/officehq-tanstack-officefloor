-- A rate may carry a to-date: it applies through that date. A claim dated after the to-date of the rate that
-- would apply to it (no later rate being in force then) has no rate. A null to-date applies without end.
ALTER TABLE rate ADD COLUMN to_date DATE;
