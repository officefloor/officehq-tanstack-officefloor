-- The sheet's alert threshold: a person whose due-total crosses above it is alerted in the audit; null: off. Not dated.
ALTER TABLE sheet ADD COLUMN alert_threshold DECIMAL(15, 2);
