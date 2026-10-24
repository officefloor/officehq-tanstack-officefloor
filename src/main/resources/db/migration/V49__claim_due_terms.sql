-- Terms count business days. A claim keeps the date and the terms its due-by was fixed with; its due-by is the
-- terms-th business day after that date (skipping Saturdays, Sundays and the sheet's holidays now), worked out again
-- whenever the sheet's holidays change. Existing claims take their date and the days from it to their due-by; their
-- due-by is worked out again by the next migration.
ALTER TABLE claim ADD COLUMN due_from DATE;
ALTER TABLE claim ADD COLUMN due_terms INT;
UPDATE claim SET due_from = claim_date, due_terms = GREATEST(0, DATEDIFF('DAY', claim_date, due_by));
ALTER TABLE claim ALTER COLUMN due_from SET NOT NULL;
ALTER TABLE claim ALTER COLUMN due_terms SET NOT NULL;
