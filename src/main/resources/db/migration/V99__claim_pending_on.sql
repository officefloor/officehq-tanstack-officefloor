-- The sheet's today a claim last went to PENDING on (its pending day); null when it never went to PENDING. With the
-- sheet's pending-sla it tells whether a PENDING claim is late. Kept in claim_hist too; the history trigger is
-- set again afterwards, as in V94__claim_review_user.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN pending_on DATE;
ALTER TABLE claim_hist ADD COLUMN pending_on DATE;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
