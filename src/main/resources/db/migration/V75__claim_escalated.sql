-- A claim is escalated at most once ever: set when a reminder comes at least the sheet's escalation gap
-- (setting escalate-after) after its previous one. Kept in the claim's history too (see V39); the journal trigger is
-- made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_history ADD COLUMN escalated BOOLEAN;
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
