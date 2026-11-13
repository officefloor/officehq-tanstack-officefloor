-- The user a steward assigned the claim's review to; null when never assigned. Kept in claim_hist too; the history
-- trigger is set again afterwards, as in V93__claim_shares.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN review_user VARCHAR(255);
ALTER TABLE claim_hist ADD COLUMN review_user VARCHAR(255);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
