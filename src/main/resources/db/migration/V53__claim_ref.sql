-- A claim may carry an external reference (null: none), unique on its sheet without regard to case: ref_key is it
-- lower-cased, and the unique index lets any number of claims have none.
ALTER TABLE claim ADD COLUMN ref VARCHAR(200);
ALTER TABLE claim ADD COLUMN ref_key VARCHAR(200) GENERATED ALWAYS AS LOWER(ref);
CREATE UNIQUE INDEX claim_sheet_ref ON claim(sheet_id, ref_key);

ALTER TABLE claim_history ADD COLUMN ref VARCHAR(200);
