-- A claim's external reference (null when none was given), kept as typed with its surrounding spaces removed. No two
-- claims of a sheet share one, comparing without regard to case: ref_key is the reference in lower case.
ALTER TABLE claim ADD COLUMN ref VARCHAR(250);
ALTER TABLE claim ADD COLUMN ref_key VARCHAR(250);
CREATE UNIQUE INDEX claim_ref_unique ON claim (sheet_id, ref_key);
ALTER TABLE claim_edit ADD COLUMN ref VARCHAR(250);
