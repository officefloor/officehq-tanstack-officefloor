-- Who last edited a claim (through the claim form or the row's amount entry; NULL: never edited). Like its opener,
-- they may not settle or approve it.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(100);
