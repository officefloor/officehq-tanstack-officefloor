-- The user who last edited a claim (null: never edited). Like its opener, they may not settle or approve it.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(200);
