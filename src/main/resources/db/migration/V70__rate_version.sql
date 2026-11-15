-- A rate is kept as versions: setting a rate for a grade and from-date that already has one keeps the
-- value it replaces here as an older version, and the rate (the newest version, the one in force)
-- carries its version number (how many versions it has, counting itself).
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE rate_history ADD COLUMN version INT NOT NULL DEFAULT 1;
CREATE TABLE rate_version (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    grade     VARCHAR(10)    NOT NULL,
    version   INT            NOT NULL,
    pct       DECIMAL(19, 2) NOT NULL,
    from_date DATE,
    to_date   DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01')),
    set_by    VARCHAR(200)   NOT NULL,
    set_at    TIMESTAMP      NOT NULL
);
CREATE INDEX rate_version_sheet ON rate_version (sheet_id, grade, from_key, version);
