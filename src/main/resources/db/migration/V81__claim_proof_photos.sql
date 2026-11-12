-- How many of a claim's proofs are photos; the rest of its proofs are receipts (a proof added with no kind is a
-- receipt). Kept in claim_hist too; the history trigger is set again afterwards, as in V48__claim_edited_by.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
ALTER TABLE claim_hist ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
