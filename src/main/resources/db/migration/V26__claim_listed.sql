-- Claims are listed newest first by `listed`: a new claim takes the next value of claim_listed, and
-- a claim reassigned to another person takes the next one again, so it is listed as that person's
-- newest claim whatever its date. Claims already kept are listed by id, as before.
CREATE SEQUENCE claim_listed START WITH 1000000000;
ALTER TABLE claim ADD COLUMN listed BIGINT DEFAULT NEXT VALUE FOR claim_listed;
UPDATE claim SET listed = id;
ALTER TABLE claim ALTER COLUMN listed SET NOT NULL;
CREATE INDEX claim_sheet_listed ON claim (sheet_id, listed);
