-- Who last edited a claim (through its form or its amount entry): the signed-in name. Null while it was never edited.
-- Like its opener, the one who last edited a claim cannot settle or approve it.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(255);
