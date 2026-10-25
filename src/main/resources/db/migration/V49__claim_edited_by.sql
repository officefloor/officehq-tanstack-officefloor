-- The user who last edited the claim through its form or amount entry (null: never edited); like its opener, they may not settle or approve it.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(200);
ALTER TABLE hist.claim ADD COLUMN edited_by VARCHAR(200);
ALTER TABLE asof.claim ADD COLUMN edited_by VARCHAR(200);
