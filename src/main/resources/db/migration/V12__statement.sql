-- A statement taken for a person records their due-total at that moment and binds each of their
-- claims that counts then to the figure it had: a bound claim shows and totals stated_due (NULL:
-- n/a, no rate then) whatever rates or grades do later, until the person's next statement rebinds
-- their counting claims and unbinds the rest. A settled claim's settled_due still comes first.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(12, 2);
-- statement_count: statements taken for the person; stated_total: the last one's due-total (NULL:
-- none taken, or n/a); statement_date: the sheet's today when the last one was taken.
ALTER TABLE person ADD COLUMN statement_count INT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total DECIMAL(12, 2);
ALTER TABLE person ADD COLUMN statement_date DATE;
