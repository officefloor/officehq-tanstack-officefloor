-- Where a claim is listed: claims are listed newest first by this, not by id, so a claim moved to
-- another person is listed as that person's newest. A claim takes the sheet's next place when it is
-- made and again when it is moved; the sheet keeps the last place given.
ALTER TABLE sheet ADD COLUMN last_claim_listed BIGINT NOT NULL DEFAULT 0;
ALTER TABLE claim ADD COLUMN listed BIGINT NOT NULL DEFAULT 0;
UPDATE claim SET listed = id;
UPDATE sheet SET last_claim_listed = COALESCE((SELECT MAX(c.id) FROM claim c WHERE c.sheet_id = sheet.id), 0);
