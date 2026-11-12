-- Every version of the rows a person's figures follow, so a statement can be taken as of a past date:
-- each insert, update or delete of a claim, person, grade, hold, rate or setting is kept in <table>_hist
-- (by the trigger net.officefloor.hq.app.history.HistoryTrigger) with the sheet's today when it was made
-- (hist_made_on), in the order made (hist_seq); a deleted row is kept with hist_gone. A row as it stood at
-- the end of a date is its last version made while the sheet's today was on or before that date.
-- A column later added to one of these tables must be added to its _hist table too.
CREATE SEQUENCE hist_seq;

CREATE TABLE claim_hist AS SELECT * FROM claim WITH NO DATA;
CREATE TABLE person_hist AS SELECT * FROM person WITH NO DATA;
CREATE TABLE person_grade_hist AS SELECT * FROM person_grade WITH NO DATA;
CREATE TABLE person_hold_hist AS SELECT * FROM person_hold WITH NO DATA;
CREATE TABLE rate_hist AS SELECT * FROM rate WITH NO DATA;
CREATE TABLE sheet_setting_hist AS SELECT * FROM sheet_setting WITH NO DATA;
CREATE TABLE sheet_setting_from_hist AS SELECT * FROM sheet_setting_from WITH NO DATA;

ALTER TABLE claim_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE claim_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE claim_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE person_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE person_grade_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_grade_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_grade_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE person_hold_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE person_hold_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE person_hold_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE rate_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE rate_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE rate_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE sheet_setting_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE sheet_setting_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE sheet_setting_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE sheet_setting_from_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE sheet_setting_from_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE sheet_setting_from_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX claim_hist_key ON claim_hist (id, hist_seq);
CREATE INDEX person_hist_key ON person_hist (id, hist_seq);
CREATE INDEX person_grade_hist_key ON person_grade_hist (person_id, from_date, hist_seq);
CREATE INDEX person_hold_hist_key ON person_hold_hist (id, hist_seq);
CREATE INDEX rate_hist_key ON rate_hist (sheet_id, grade, from_date, hist_seq);
CREATE INDEX sheet_setting_hist_key ON sheet_setting_hist (sheet_id, setting_key, hist_seq);
CREATE INDEX sheet_setting_from_hist_key ON sheet_setting_from_hist (sheet_id, setting_key, from_date, hist_seq);

CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER person_grade_history AFTER INSERT, UPDATE, DELETE ON person_grade FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER person_hold_history AFTER INSERT, UPDATE, DELETE ON person_hold FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON rate FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON sheet_setting FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
CREATE TRIGGER sheet_setting_from_history AFTER INSERT, UPDATE, DELETE ON sheet_setting_from FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
