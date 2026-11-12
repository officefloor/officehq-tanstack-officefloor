-- A person's past retirements kept in person_retirement_hist, as person_hold is, so a sheet read as of a
-- date shows only the retirements they had been unretired from by then. A retirement already recorded is
-- kept as made on its to-date (the sheet's today when they were unretired).
CREATE TABLE person_retirement_hist AS SELECT * FROM person_retirement WITH NO DATA;
ALTER TABLE person_retirement_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_retirement_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_retirement_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX person_retirement_hist_key ON person_retirement_hist (id, hist_seq);
INSERT INTO person_retirement_hist (id, sheet_id, person_id, from_date, to_date, hist_seq, hist_made_on)
    SELECT id, sheet_id, person_id, from_date, to_date, NEXT VALUE FOR hist_seq, to_date FROM person_retirement ORDER BY id;
CREATE TRIGGER person_retirement_history AFTER INSERT, UPDATE, DELETE ON person_retirement FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
