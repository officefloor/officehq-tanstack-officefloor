-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the value it
-- replaces as an older version and makes the new one the next. The rate row holds the newest (the one in force) and
-- its version number (how many versions it has); rate_version holds each older one, as it stood when replaced.
ALTER TABLE rate ADD COLUMN version INTEGER DEFAULT 1 NOT NULL;
CREATE TABLE rate_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    grade VARCHAR(10) NOT NULL,
    from_date DATE,
    version INTEGER NOT NULL,
    pct DECIMAL(19, 2) NOT NULL,
    to_date DATE,
    replaced_on DATE NOT NULL
);
CREATE INDEX rate_version_sheet ON rate_version (sheet_id, grade, id);
