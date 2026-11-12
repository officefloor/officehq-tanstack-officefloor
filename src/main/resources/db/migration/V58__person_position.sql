-- A person's place in the list: the people are listed by position (lowest first). A new person goes to
-- the end (one past the highest position on the sheet); moving a person up or down swaps their position
-- with the neighbour's. Existing people keep the order they were added. Kept in person_hist too; the
-- history trigger is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER person_history;
ALTER TABLE person ADD COLUMN list_position BIGINT;
ALTER TABLE person_hist ADD COLUMN list_position BIGINT;
UPDATE person SET list_position = id;
UPDATE person_hist SET list_position = id;
ALTER TABLE person ALTER COLUMN list_position SET NOT NULL;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
