-- A claim's due-by is fixed when it is added: its date plus the terms of its person's grade in force on
-- that date (else the sheet's terms). Claims already kept take their date plus the sheet's terms now.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD('DAY', COALESCE((SELECT CAST(s.setting_value AS INT) FROM sheet_setting s
    WHERE s.sheet_id = c.sheet_id AND s.setting_key = 'terms'), 14), c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
