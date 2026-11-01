-- A claim's due-by, fixed when it is added: its date plus the terms of its person's grade in force on that date
-- (the setting terms-<grade>, else the sheet's terms). It does not move when the terms or the claim's date change.
-- A claim already kept takes its date plus the sheet's terms as they stand (14 when none is set).
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD(DAY, COALESCE((SELECT CAST(s.setting_value AS INT) FROM setting s
    WHERE s.sheet = claim.sheet AND s.setting_key = 'terms' AND s.setting_value <> ''), 14), claimed_on);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
