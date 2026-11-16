-- Where a claim is listed among the sheet's claims (newest first, highest first): its id when added, and one more
-- than any on the sheet when it is moved to another person, so it is listed as that person's newest claim.
ALTER TABLE claim ADD COLUMN listed BIGINT;
UPDATE claim SET listed = id;
