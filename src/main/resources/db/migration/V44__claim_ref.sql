-- A claim's external reference: the text typed for it, spaces around it removed; null when none was given.
-- No two claims on a sheet carry the same reference, compared without regard to case (checked by
-- net.officefloor.hq.app.claim.CheckRef). Kept in claim_hist too (see V39__history.sql); the history
-- trigger is set again afterwards, as in V41__claim_reminded.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN ref VARCHAR(255);
ALTER TABLE claim_hist ADD COLUMN ref VARCHAR(255);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE INDEX claim_ref ON claim (sheet_id, ref);
