-- A sheet's periods: a name, unique on the sheet without regard to case (stored trimmed), and a
-- from- and to-date, both inclusive (from on or before to). No two periods of a sheet overlap; the
-- app keeps that, as the store cannot.
CREATE TABLE period (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name      VARCHAR(500) NOT NULL,
    name_key  VARCHAR(500) GENERATED ALWAYS AS (LOWER(name)),
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    CHECK (from_date <= to_date)
);
CREATE UNIQUE INDEX period_sheet_name ON period (sheet_id, name_key);
