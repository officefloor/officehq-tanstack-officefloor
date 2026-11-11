-- A rate is kept as versions: setting a rate for a grade and from-date that already have one keeps
-- the value it replaces as an older version and adds a new one. The rate row holds the newest
-- version (the one in force) and its number, counted from 1; rate_version holds the older ones.
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;
CREATE TABLE rate_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    grade CHAR(1) NOT NULL,
    from_date DATE,
    version INT NOT NULL,
    pct DECIMAL(19, 2) NOT NULL,
    to_date DATE
);
CREATE INDEX rate_version_sheet ON rate_version(sheet_id, grade);
