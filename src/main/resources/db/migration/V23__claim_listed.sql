-- Where a claim is listed among the claims of its sheet: newest first by this. A claim takes the sheet's next one
-- when it is added, and again when it is reassigned, so a moved claim lists as its new person's newest whatever its date.
ALTER TABLE claim ADD COLUMN listed BIGINT;
UPDATE claim SET listed = id;
