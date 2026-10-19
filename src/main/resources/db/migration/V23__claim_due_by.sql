-- A claim's due-by is fixed when it is created: its date plus the terms of its person's grade in force on
-- that date (the grade's own terms, else the sheet's). It does not move when the terms or the claim's date
-- change later. Claims made before the grade terms existed get their date plus the sheet's terms (14 unset).
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD(DAY, COALESCE((SELECT CAST(s.val AS INT) FROM sheet_setting s
    WHERE s.sheet_id = c.sheet_id AND s.setting = 'terms'), 14), c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
