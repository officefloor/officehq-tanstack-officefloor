-- A person's statements: how many have been taken, and the last one's due-total (as shown, so
-- possibly 'n/a') and date (the sheet's today when it was taken). Null until the first statement.
ALTER TABLE person ADD COLUMN statement_count BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total VARCHAR(20);
ALTER TABLE person ADD COLUMN statement_date DATE;
-- A claim bound by its person's last statement: it shows and totals stated_due (the figure it had
-- then; null when it had none, its due then staying live) until the next statement rebinds or
-- unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(19, 2);
