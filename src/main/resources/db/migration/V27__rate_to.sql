-- A rate may apply through a to-date (inclusive); null when it has no end. A date after it, with no
-- later rate of the grade in force, has no rate.
ALTER TABLE rate ADD COLUMN to_date DATE;
-- A claim a statement bound while the rate that would apply to it had ended keeps n/a.
ALTER TABLE claim ADD COLUMN stated_na BOOLEAN DEFAULT FALSE NOT NULL;
