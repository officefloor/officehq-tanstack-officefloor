-- A claim's due-by: fixed when the claim is added, its date plus the terms of its person's grade in force on that
-- date (the sheet's terms for a grade without its own). Claims already stored take their date plus the sheet's terms.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD(DAY, COALESCE((SELECT CAST(s.setting_value AS INT) FROM setting s
    WHERE s.sheet_id = claim.sheet_id AND s.setting_key = 'terms'), 14), claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
