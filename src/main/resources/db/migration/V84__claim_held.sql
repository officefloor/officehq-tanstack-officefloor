-- Whether the claim is held: while OPEN (or PENDING) and unbound by a statement it counts in no live total. Kept in
-- claim_hist too; the history trigger is set again afterwards, as in V79__claim_waived.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_hist ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
