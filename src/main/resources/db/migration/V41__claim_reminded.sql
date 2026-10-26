-- An OPEN or PENDING claim can be reminded, once per sheet's today: the last sheet's today a reminder was written
-- (null: never reminded). Kept in the claim's history too (see V39). The journal trigger is made again around the
-- change: altering the table copies it, and a trigger left on would keep naming the copy's history.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN reminded_on DATE;
ALTER TABLE claim_history ADD COLUMN reminded_on DATE;
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
