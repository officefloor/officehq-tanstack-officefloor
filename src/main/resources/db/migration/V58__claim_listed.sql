-- Where a claim stands in its person's list: claims are listed newest first by when they came to their person, on
-- being added or on being moved to them from another person (a moved claim is the newest of its new person's).
CREATE SEQUENCE claim_listed_seq START WITH 1;
ALTER TABLE claim ADD COLUMN listed BIGINT DEFAULT NEXT VALUE FOR claim_listed_seq NOT NULL;
UPDATE claim SET listed = id;
