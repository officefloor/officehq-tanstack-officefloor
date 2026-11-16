-- A rate may carry a to-date: it applies through that date. A claim dated after the to-date of the
-- rate that would apply to it (the latest from-date on or before its date) has no rate. Null: no end.
ALTER TABLE rate ADD COLUMN to_date DATE;
