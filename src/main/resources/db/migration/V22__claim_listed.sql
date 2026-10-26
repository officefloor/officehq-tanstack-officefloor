-- Where a claim stands in its sheet's newest-first list: the next on its sheet when it is made, and again
-- when it is reassigned to another person, so a moved claim lists as its new person's newest whatever its date.
ALTER TABLE claim ADD COLUMN listed BIGINT;
UPDATE claim SET listed = id;
ALTER TABLE claim ALTER COLUMN listed SET NOT NULL;
CREATE INDEX claim_sheet_listed ON claim (sheet_id, listed);
