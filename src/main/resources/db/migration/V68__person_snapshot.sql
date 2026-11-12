-- A person's snapshots: one per SNAPSHOT written for them when the sheet's today is moved on, with the day
-- being left and their due-total as it stood then (as shown: two decimals or n/a). Kept in
-- person_snapshot_hist, as person_retirement is (see V62__person_retirement_history.sql), so a sheet read as
-- of a date shows only the snapshots written by then.
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    left_day  DATE         NOT NULL,
    due_total VARCHAR(32)  NOT NULL
);
CREATE INDEX person_snapshot_sheet ON person_snapshot (sheet_id, person_id);
CREATE TABLE person_snapshot_hist AS SELECT * FROM person_snapshot WITH NO DATA;
ALTER TABLE person_snapshot_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_snapshot_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_snapshot_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX person_snapshot_hist_key ON person_snapshot_hist (id, hist_seq);
CREATE TRIGGER person_snapshot_history AFTER INSERT, UPDATE, DELETE ON person_snapshot FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
