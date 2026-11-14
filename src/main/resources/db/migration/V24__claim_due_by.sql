-- A claim's due-by, fixed when it is added: its date plus the terms of its person's grade in force on
-- its date (a grade's own terms, setting terms-<grade>, else the sheet's terms). It does not change
-- when the terms or its date change later. Claims already added take their date plus the sheet's
-- terms now (14 when never set).
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD(DAY, COALESCE((SELECT CAST(s.val AS INT) FROM sheet_setting s
    WHERE s.sheet_id = claim.sheet_id AND s.setting = 'terms'), 14), claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
