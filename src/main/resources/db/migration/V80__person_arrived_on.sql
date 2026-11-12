-- The sheet's today on which a transfer brought the person here: set with origin on the copy a transfer makes,
-- null for anyone added on the sheet itself. A transfer is undone only on that day. Kept in person_hist too;
-- the history trigger is set again afterwards, as in V59__person_origin.sql.
DROP TRIGGER person_history;
ALTER TABLE person ADD COLUMN arrived_on DATE;
ALTER TABLE person_hist ADD COLUMN arrived_on DATE;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
