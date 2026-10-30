-- A rate can carry a to-date: it applies through that date. A claim whose rate (the one with the
-- latest from-date on or before its date) ended before the claim's date has no rate, so its due is
-- n/a. A null to-date applies with no end.
ALTER TABLE rate ADD COLUMN to_date DATE;
