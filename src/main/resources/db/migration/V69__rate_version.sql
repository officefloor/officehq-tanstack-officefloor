-- A rate is kept as versions: setting a rate again for a grade and from-date keeps the value it replaces as an older
-- version (rate_version) and makes the new one the next version. The rate row holds the newest version, the one in
-- force; its version is how many versions it has had.
ALTER TABLE rate ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE hist.rate ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE asof.rate ADD COLUMN version INT NOT NULL DEFAULT 1;
CREATE TABLE rate_version (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    grade     VARCHAR(1)    NOT NULL,
    from_date DATE,
    version   INT           NOT NULL,
    rate      DECIMAL(5, 2) NOT NULL,
    to_date   DATE
);
CREATE INDEX rate_version_rate ON rate_version (sheet_id, grade, from_date);
