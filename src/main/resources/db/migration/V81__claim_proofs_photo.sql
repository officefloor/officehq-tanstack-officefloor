-- How many of a claim's proofs are photos (proofs_photo); the rest of its proofs are receipts. 0 for a claim given none.
-- Kept in the claim's history too (see V39); the journal trigger is made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN proofs_photo INT DEFAULT 0 NOT NULL;
ALTER TABLE claim_history ADD COLUMN proofs_photo INT;
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
