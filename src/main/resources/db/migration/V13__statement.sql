-- A person's statements: how many have been taken, and the last one's due-total (NULL when it was n/a,
-- or none was taken) and date (the sheet's today when it was taken; NULL when none was taken).
ALTER TABLE person ADD COLUMN statement_count INT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total DECIMAL(14, 2);
ALTER TABLE person ADD COLUMN statement_date DATE;
-- Whether a claim is bound by its person's last statement (it counted then), and the figure it was bound
-- to (NULL when its figure was n/a then: its due stays live). The person's next statement rebinds or unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(14, 2);
