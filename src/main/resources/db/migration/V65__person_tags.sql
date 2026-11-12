-- A person's tags: as a claim's (see V45__claim_tags.sql), each trimmed and lower-cased, repeats dropped,
-- sorted and comma separated (no spaces); null when they have none. Kept in person_hist too; the history
-- trigger is set again afterwards, as in V51__person_contact.sql.
DROP TRIGGER person_history;
ALTER TABLE person ADD COLUMN tags VARCHAR(4000);
ALTER TABLE person_hist ADD COLUMN tags VARCHAR(4000);
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
