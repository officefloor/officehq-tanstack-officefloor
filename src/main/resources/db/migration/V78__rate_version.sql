-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the value it
-- replaces in rate_version and stores the new one in rate with the next version (the first is 1). The row in rate is
-- always the newest version, the one in force; version is how many versions the grade and from-date has.
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;

-- The older versions of each rate, as they were before the next one replaced them.
CREATE TABLE rate_version (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200)  NOT NULL,
    grade      VARCHAR(1)    NOT NULL,
    rate       DECIMAL(7, 2) NOT NULL,
    from_date  DATE,
    to_date    DATE,
    version    INT           NOT NULL
);
CREATE INDEX rate_version_sheet ON rate_version (sheet_id, grade, id);
