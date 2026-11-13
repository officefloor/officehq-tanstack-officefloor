-- The grades added to a sheet beyond A, B and C: a code of one to three capital letters, each at most once per
-- sheet, listed after A, B and C in the order added. Not kept by version: a grade added is a grade of the sheet
-- on every date, also as the screen is read as of a date. A grade is kept in person_grade, rate and rate_version
-- (and their history) up to three letters; the history triggers are set again afterwards, as in V70__rate_versions.sql.
CREATE TABLE grade_added (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255) NOT NULL,
    grade    VARCHAR(16)  NOT NULL,
    UNIQUE (sheet_id, grade)
);

DROP TRIGGER person_grade_history;
ALTER TABLE person_grade ALTER COLUMN grade VARCHAR(16) NOT NULL;
ALTER TABLE person_grade_hist ALTER COLUMN grade VARCHAR(16) NOT NULL;
CREATE TRIGGER person_grade_history AFTER INSERT, UPDATE, DELETE ON person_grade FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

DROP TRIGGER rate_history;
ALTER TABLE rate ALTER COLUMN grade VARCHAR(16) NOT NULL;
ALTER TABLE rate_hist ALTER COLUMN grade VARCHAR(16) NOT NULL;
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON rate FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
ALTER TABLE rate_version ALTER COLUMN grade VARCHAR(16) NOT NULL;
