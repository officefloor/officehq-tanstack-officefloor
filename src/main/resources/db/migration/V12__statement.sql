-- A person's statements: how many have been taken, and the last one's due-total (null when it was
-- n/a, or when none has been taken) and date (the sheet's today when it was taken).
ALTER TABLE person ADD COLUMN statement_count INT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total DECIMAL(14,2);
ALTER TABLE person ADD COLUMN statement_date DATE;
-- A claim that counted at its person's last statement is bound (`stated`) to the figure it had
-- then (`stated_due`, null when that was n/a): it shows and totals that figure, whatever later
-- rates or grades would make it, until the next statement rebinds or unbinds it. A settled
-- claim's fixed `due` still comes first.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(14,2);
