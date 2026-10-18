-- A grade may have its own terms (the terms-<grade> setting); a grade without uses the sheet's terms.
-- A claim's due-by is fixed when it is created: its date plus the terms of its person's grade in force
-- on that date. Later changes to the terms or the claim's date do not alter it. Existing claims take
-- it from their date and the sheet's terms now (14 days when none is set).
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD('DAY', COALESCE(
    (SELECT CAST(s.setting_value AS INT) FROM setting s WHERE s.sheet_id = c.sheet_id AND s.setting_key = 'terms'), 14),
    c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
