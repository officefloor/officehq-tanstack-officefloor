-- The signed-in user who last edited a claim (its form or its row's amount entry); null when never edited.
-- Like its opener, that user may not settle or approve it.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(100);
