-- Terms count business days. A claim keeps the terms it was fixed with when it was created (terms_days) and the date
-- they count from (due_from: its date then; a later change of its date leaves it). Its due-by is no longer stored: it
-- is the terms_days-th business day after due_from, skipping Saturdays, Sundays and the sheet's holidays as they are
-- now. A claim stored before this keeps, as its terms, the days between its date and the due-by it had.
ALTER TABLE claim ADD COLUMN due_from DATE;
ALTER TABLE claim ADD COLUMN terms_days INT;
UPDATE claim SET due_from = claim_date, terms_days = GREATEST(0, DATEDIFF(DAY, claim_date, due_by));
ALTER TABLE claim ALTER COLUMN due_from SET NOT NULL;
ALTER TABLE claim ALTER COLUMN terms_days SET NOT NULL;
ALTER TABLE claim DROP COLUMN due_by;

ALTER TABLE claim_history ADD COLUMN due_from DATE;
ALTER TABLE claim_history ADD COLUMN terms_days INT;
UPDATE claim_history SET due_from = claim_date, terms_days = GREATEST(0, DATEDIFF(DAY, claim_date, due_by));
ALTER TABLE claim_history ALTER COLUMN due_from SET NOT NULL;
ALTER TABLE claim_history ALTER COLUMN terms_days SET NOT NULL;
ALTER TABLE claim_history DROP COLUMN due_by;
