-- Whether a steward waived the claim's late charge: a waived claim bears no late charge from then on. Kept in
-- claim_hist too; the history trigger is set again afterwards, as in V48__claim_edited_by.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim_hist ADD COLUMN waived BOOLEAN DEFAULT FALSE NOT NULL;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
