-- Where a claim is listed: claims are listed newest first by this key, which a new claim takes on insert
-- and a reassigned claim takes afresh, so it lists as its new person's newest claim whatever its date.
CREATE SEQUENCE claim_listed_seq START WITH 1;
ALTER TABLE claim ADD COLUMN listed BIGINT DEFAULT NEXT VALUE FOR claim_listed_seq NOT NULL;
