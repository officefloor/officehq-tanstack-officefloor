-- A statement binds each of a person's claims that count at that moment to the figure it has then
-- (stated_due; null when that figure was n/a): from then on the claim shows and totals that figure
-- whatever rates or grades do, until the person's next statement rebinds it. A claim that does not
-- count at a statement is left unbound (stated false, its due live again). A settled claim's
-- settled_due still comes first.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(14,2);
-- The person's statements: how many were taken, and the last one's due-total (null when it was
-- n/a) and date (the sheet's today when it was taken).
ALTER TABLE person ADD COLUMN statement_count INT DEFAULT 0 NOT NULL;
ALTER TABLE person ADD COLUMN stated_total DECIMAL(14,2);
ALTER TABLE person ADD COLUMN statement_date DATE;
