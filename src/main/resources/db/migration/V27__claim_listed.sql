-- Where a claim is listed under its person: newest first by this key (then by id). A new claim takes the next
-- key; a claim moved to another person takes the next key again, so it is listed as that person's newest claim.
CREATE SEQUENCE claim_listed_seq START WITH 1;
ALTER TABLE claim ADD COLUMN listed BIGINT;
UPDATE claim SET listed = NEXT VALUE FOR claim_listed_seq;
ALTER TABLE claim ALTER COLUMN listed SET DEFAULT NEXT VALUE FOR claim_listed_seq;
ALTER TABLE claim ALTER COLUMN listed SET NOT NULL;
