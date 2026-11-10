-- A rate may carry a to-date: it applies through it, and a claim dated after it with no later
-- from-date for the grade has no rate. A null to-date applies with no end.
ALTER TABLE rate ADD COLUMN to_date DATE;
