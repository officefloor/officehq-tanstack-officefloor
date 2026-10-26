-- The user who last edited a claim (through its form or its row's amount entry; null when never edited). Like its
-- opener, they cannot settle or approve it. Kept in the claim's history too (see V39). The journal trigger is made
-- again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(255);
ALTER TABLE claim_history ADD COLUMN edited_by VARCHAR(255);
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
