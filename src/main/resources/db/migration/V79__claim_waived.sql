-- A steward may waive an OPEN or PENDING claim's late charge, once: a waived claim bears no late charge from then on.
-- Kept in the claim's history too (see V39); the journal trigger is made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_history ADD COLUMN waived BOOLEAN;
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
