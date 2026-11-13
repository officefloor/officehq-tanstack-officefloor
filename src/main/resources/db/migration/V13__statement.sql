-- A person's statement records their due-total at that moment and binds each of their counting
-- claims to the figure it has then. The person keeps how many statements were taken, the last one's
-- due-total ('n/a' when it had none) and the sheet's today when it was taken.
ALTER TABLE person ADD COLUMN statement_count INT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total VARCHAR(30);
ALTER TABLE person ADD COLUMN statement_date DATE;
-- A stated claim shows and totals its bound figure (stated_due; null: n/a) until its person's next
-- statement rebinds it, or unbinds it (live again) when it no longer counts.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(25, 2);
