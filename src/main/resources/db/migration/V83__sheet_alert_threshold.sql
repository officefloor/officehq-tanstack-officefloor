-- The sheet's alert threshold (two decimals; null: off; not dated). A person whose due-total crosses above it
-- over a change is alerted in the audit file.
ALTER TABLE sheet ADD COLUMN alert_threshold DECIMAL(14, 2);
