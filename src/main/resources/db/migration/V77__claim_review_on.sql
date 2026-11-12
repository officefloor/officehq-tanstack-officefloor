-- The sheet's today a claim went to REVIEW on (its review day); null when it never went to REVIEW. With the
-- sheet's review-sla it tells whether a REVIEW claim is late. Kept in claim_hist too; the history trigger is
-- set again afterwards, as in V48__claim_edited_by.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN review_on DATE;
ALTER TABLE claim_hist ADD COLUMN review_on DATE;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
