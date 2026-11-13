-- The user who last edited a claim through its editor or its row's amount entry (four eyes: they cannot settle or
-- approve it). Null until the claim is first edited.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(255);
