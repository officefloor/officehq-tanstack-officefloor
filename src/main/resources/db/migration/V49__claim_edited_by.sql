-- Who last edited the claim (through its form or its row's amount entry); null when never edited. Like its opener,
-- that user cannot settle nor approve it (four eyes).
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(200);
