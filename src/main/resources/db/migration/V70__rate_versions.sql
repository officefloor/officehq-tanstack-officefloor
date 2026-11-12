-- A rate is kept as versions: setting a rate for a grade and from-date that already exist keeps the value it
-- replaces as an older version in rate_version and adds a new one. The rate row holds the newest version (the
-- one in force) and its number (version), which counts its versions. Kept in rate_hist too; the history
-- trigger is set again afterwards, as in V69__period_cap.sql.
DROP TRIGGER rate_history;
ALTER TABLE rate ADD COLUMN version INT DEFAULT 1 NOT NULL;
ALTER TABLE rate_hist ADD COLUMN version INT DEFAULT 1 NOT NULL;
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON rate FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

-- Every version of a rate, numbered from 1 in the order set; the newest is the one in the rate table.
CREATE TABLE rate_version (
    sheet_id  VARCHAR(255)   NOT NULL,
    grade     VARCHAR(1)     NOT NULL,
    from_date DATE           NOT NULL,
    version   INT            NOT NULL,
    pct       DECIMAL(19, 2) NOT NULL,
    to_date   DATE,
    PRIMARY KEY (sheet_id, grade, from_date, version)
);
INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date)
    SELECT sheet_id, grade, from_date, 1, pct, to_date FROM rate;
