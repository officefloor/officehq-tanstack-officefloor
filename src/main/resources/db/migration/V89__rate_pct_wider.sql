-- A rate may be a percent up to 99999.99.
ALTER TABLE rate ALTER COLUMN pct DECIMAL(7, 2);
ALTER TABLE rate_log ALTER COLUMN pct DECIMAL(7, 2);
