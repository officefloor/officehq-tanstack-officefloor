-- Who last edited a claim (through its form or its amount entry): null when never edited. That user, like its
-- opener, may not settle or approve it (four eyes).
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(200);
