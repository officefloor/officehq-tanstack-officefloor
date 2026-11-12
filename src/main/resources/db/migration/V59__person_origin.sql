-- The sheet a person was transferred from: set on the copy a transfer makes on the other sheet, null for
-- anyone added on the sheet itself. Kept in person_hist too; the history trigger is set again afterwards, as
-- in V45__claim_tags.sql.
DROP TRIGGER person_history;
ALTER TABLE person ADD COLUMN origin VARCHAR(255);
ALTER TABLE person_hist ADD COLUMN origin VARCHAR(255);
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
