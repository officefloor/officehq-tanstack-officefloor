-- The user who last edited a claim (through the claim form or the row's amount entry); null if it was never edited.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(255);
