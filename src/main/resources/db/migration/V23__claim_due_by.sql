-- A claim's due-by, fixed when it is made: its date plus the terms of its person's grade on that date
-- (the sheet's terms when the grade has none of its own). Claims made before terms were kept per grade
-- are due by their date plus the sheet's terms (14 when never set).
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD(DAY, COALESCE((SELECT CAST(s.setting_value AS INT) FROM setting s
    WHERE s.sheet_id = c.sheet_id AND s.setting_key = 'terms' AND s.setting_value <> ''), 14), c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
