-- Where a claim is listed: claims are listed newest first by this key, not by id, so a claim moved to
-- another person is listed as that person's newest. A claim takes the sheet's next key when it is
-- added or moved; the sheet keeps the last key it gave.
ALTER TABLE sheet ADD COLUMN last_claim_listed BIGINT NOT NULL DEFAULT 0;
ALTER TABLE claim ADD COLUMN listed BIGINT;
UPDATE claim SET listed = id;
ALTER TABLE claim ALTER COLUMN listed SET NOT NULL;
UPDATE sheet SET last_claim_listed = COALESCE((SELECT MAX(c.id) FROM claim c WHERE c.sheet_id = sheet.id), 0);
CREATE INDEX claim_listed ON claim (sheet_id, person_id, listed);
