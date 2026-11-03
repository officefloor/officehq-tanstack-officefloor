-- A sheet's alert threshold (two decimals; null means off; not dated): a person whose due-total crosses above it
-- is written an ALERT line.
ALTER TABLE sheet ADD COLUMN alert_threshold DECIMAL(12, 2);
