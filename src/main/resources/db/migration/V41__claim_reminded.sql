-- The last sheet's today a reminder was written for an OPEN or PENDING claim; null when never reminded.
-- Kept in claim_hist too (see V39__history.sql). H2 re-initialises the history trigger under the
-- table's temporary copy name while it adds a column, so the trigger is set again afterwards.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN reminded_on DATE;
ALTER TABLE claim_hist ADD COLUMN reminded_on DATE;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
