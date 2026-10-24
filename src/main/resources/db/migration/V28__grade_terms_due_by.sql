-- The sheet's terms per grade (A, B, C): a claim of a person with that grade is due by its date plus this many days.
-- Null (blank) means the grade uses the sheet's terms.
ALTER TABLE sheet ADD COLUMN terms_a INT;
ALTER TABLE sheet ADD COLUMN terms_b INT;
ALTER TABLE sheet ADD COLUMN terms_c INT;
-- A claim's due-by, fixed when it is created: its date plus the terms of its person's grade in force on its date.
-- It does not change when the terms or the claim's date change later. Existing claims take their date plus the
-- sheet's terms.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD('DAY', (SELECT s.terms FROM sheet s WHERE s.id = claim.sheet_id), claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
