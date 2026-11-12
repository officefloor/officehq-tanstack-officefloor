-- A person's contact: stored trimmed and lower-cased, null when they have none. No two people on a sheet
-- share one. Kept in person_hist too; the history trigger is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER person_history;
ALTER TABLE person ADD COLUMN contact VARCHAR(255);
ALTER TABLE person_hist ADD COLUMN contact VARCHAR(255);
CREATE UNIQUE INDEX person_sheet_contact ON person (sheet_id, contact);
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
