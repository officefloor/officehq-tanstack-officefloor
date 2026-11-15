-- A claim's external reference (none: no reference), and the same lower-cased: a reference is used once
-- per sheet, compared without regard to case.
ALTER TABLE claim ADD COLUMN ref VARCHAR(200);
ALTER TABLE claim ADD COLUMN ref_key VARCHAR(200);
CREATE UNIQUE INDEX claim_sheet_ref ON claim (sheet_id, ref_key);
