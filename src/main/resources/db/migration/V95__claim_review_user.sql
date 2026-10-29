-- A steward may assign a REVIEW claim to a user: the user it was last assigned to (null when never), kept after the
-- claim leaves REVIEW. Kept in the claim's history too (see V39); the journal trigger is made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN review_user VARCHAR(255);
ALTER TABLE claim_history ADD COLUMN review_user VARCHAR(255);
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
