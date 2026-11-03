-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the value
-- it replaces as an older version (in rate_version) and puts the new one in rate with the next version
-- number. The row in rate is always the newest version, the one in force; version counts them (1 for the first).
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;
CREATE TABLE rate_version (
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    grade     VARCHAR(1)    NOT NULL,
    from_date DATE,
    version   INT           NOT NULL,
    pct       DECIMAL(7, 2) NOT NULL,
    to_date   DATE
);
CREATE INDEX rate_version_sheet_grade ON rate_version (sheet_id, grade, from_date);
