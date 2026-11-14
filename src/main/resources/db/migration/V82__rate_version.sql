-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the value it
-- replaces as an older version and adds a new one. The rate row holds the newest version (the one in force) and its
-- number, counting from 1; rate_version holds every version, the newest included.
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;
CREATE TABLE rate_version (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(10) NOT NULL,
    from_date DATE,
    version INT NOT NULL,
    rate DECIMAL(7, 2) NOT NULL,
    to_date DATE
);
CREATE INDEX rate_version_sheet_grade ON rate_version (sheet_id, grade, from_date, version);
INSERT INTO rate_version (sheet_id, grade, from_date, version, rate, to_date)
    SELECT sheet_id, grade, from_date, version, rate, to_date FROM rate;
