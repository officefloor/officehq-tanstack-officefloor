-- A person's flags kept in person_flag_hist, as person_snapshot is (see V68__person_snapshot.sql), so a sheet
-- read as of a date shows only a flag put on by then, as it stood then.
CREATE TABLE person_flag_hist AS SELECT * FROM person_flag WITH NO DATA;
ALTER TABLE person_flag_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_flag_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_flag_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX person_flag_hist_key ON person_flag_hist (sheet_id, person_id, hist_seq);
CREATE TRIGGER person_flag_history AFTER INSERT, UPDATE, DELETE ON person_flag FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
