-- An OPEN or PENDING claim may be held, and released again: while held and not bound by a statement it counts in no
-- live total, as a held person's claims do. The flag is kept whatever its status becomes.
-- Kept in the claim's history too (see V39); the journal trigger is made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_history ADD COLUMN held BOOLEAN;
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
