-- A person's statements: how many have been taken, and the last one's due-total (text: two
-- decimals, or n/a) and date (the sheet's today when it was taken). None until the first.
ALTER TABLE person ADD COLUMN statement_count INT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total VARCHAR(30);
ALTER TABLE person ADD COLUMN statement_date DATE;
-- A claim bound by its person's last statement: it shows and totals the figure it had then
-- (stated_due; none when it had none), until the next statement rebinds or unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(19, 2);
