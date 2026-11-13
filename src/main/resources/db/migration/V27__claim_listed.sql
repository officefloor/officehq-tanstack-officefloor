-- Where a claim is listed among its person's: newest (highest) first. A new claim takes the next value; a claim
-- moved to another person takes the next value again, so it is listed as its new person's newest claim.
CREATE SEQUENCE claim_listed_seq START WITH 1;
ALTER TABLE claim ADD COLUMN listed BIGINT DEFAULT NEXT VALUE FOR claim_listed_seq NOT NULL;
CREATE INDEX claim_sheet_listed ON claim (sheet_id, listed);
