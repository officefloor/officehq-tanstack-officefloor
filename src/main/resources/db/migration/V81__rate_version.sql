-- A rate is kept as versions: setting a rate for a grade and from-date already there keeps the value it
-- replaces as an older version in rate_version and puts the new one in rate with the next version
-- number. The rate in force is always the newest version, the one in rate; its version is the count.
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;
CREATE TABLE rate_version (
    sheet_id  VARCHAR(200) NOT NULL,
    grade     CHAR(1)      NOT NULL,
    from_date DATE,
    version   INT          NOT NULL,
    rate      DECIMAL(6,2) NOT NULL,
    to_date   DATE
);
