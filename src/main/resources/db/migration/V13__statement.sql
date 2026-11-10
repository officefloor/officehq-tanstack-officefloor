-- A person's statements: how many have been taken, and the last one's due-total (n/a when it had
-- none) and date (the sheet's today when it was taken). Null total and date until the first.
ALTER TABLE person ADD COLUMN statement_count INT NOT NULL DEFAULT 0;
ALTER TABLE person ADD COLUMN stated_total VARCHAR(30);
ALTER TABLE person ADD COLUMN statement_date DATE;
-- A claim bound by its person's last statement: stated, with the figure it had then (null: n/a).
-- Its person's next statement rebinds it, or leaves it unbound (live) when it does not count then.
ALTER TABLE claim ADD COLUMN stated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(19, 2);
