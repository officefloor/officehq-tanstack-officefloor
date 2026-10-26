-- A rate may carry a to-date: it applies through that date (null: with no end). Never before its from-date.
ALTER TABLE rate ADD COLUMN to_date DATE;
