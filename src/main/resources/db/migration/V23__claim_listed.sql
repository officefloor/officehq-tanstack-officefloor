-- Where a claim is listed among its person's claims, newest first: taken when it is added, and taken
-- anew when it is moved to another person, so it is listed as their newest claim whatever its date.
CREATE SEQUENCE claim_listed_seq;
ALTER TABLE claim ADD COLUMN listed BIGINT DEFAULT NEXT VALUE FOR claim_listed_seq NOT NULL;
CREATE INDEX claim_sheet_listed ON claim (sheet_id, listed);
