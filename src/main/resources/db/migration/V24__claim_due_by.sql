-- A claim's due-by is fixed when it is added: its date plus the terms of its person's grade in force on that date
-- (the grade's own terms, setting terms-<grade>, else the sheet's terms). Later changes to the terms or to the claim's
-- date leave it as it is. Existing claims take their date plus the sheet's terms (14 until they are set).
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD(DAY, COALESCE((SELECT CAST(s.setting_value AS INT) FROM sheet_setting s
    WHERE s.sheet_id = claim.sheet_id AND s.setting_key = 'terms'), 14), claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
