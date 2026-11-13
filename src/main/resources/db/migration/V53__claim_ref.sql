-- A claim's external reference (null: none), and the same lower-cased: a reference is used at most once on a sheet,
-- comparing without regard to case.
ALTER TABLE claim ADD COLUMN external_ref VARCHAR(120);
ALTER TABLE claim ADD COLUMN external_ref_key VARCHAR(120);
CREATE UNIQUE INDEX claim_external_ref_key ON claim (sheet_id, external_ref_key);
