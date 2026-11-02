-- A rate is kept as versions: setting a rate for a grade and from-date already there keeps the value it replaces
-- as an older version (rate_version) and makes the new one the rate in force, its version one more. The rate row
-- is always the newest version; version counts them all (the newest included).
ALTER TABLE rate ADD COLUMN version INT DEFAULT 1 NOT NULL;
CREATE TABLE rate_version (
    sheet     VARCHAR(200)  NOT NULL,
    grade     VARCHAR(1)    NOT NULL,
    from_date DATE,
    version   INT           NOT NULL,
    pct       DECIMAL(9, 2) NOT NULL,
    to_date   DATE
);
CREATE INDEX rate_version_sheet ON rate_version (sheet, grade, from_date);
