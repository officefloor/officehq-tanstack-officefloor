-- A sheet's periods: a name unique on the sheet (ignoring case; stored trimmed) and a from and to
-- date, both inclusive. No two periods on a sheet overlap. A claim belongs to the period whose dates
-- contain its date, or to none.
CREATE TABLE period (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    name VARCHAR(100) NOT NULL,
    name_key VARCHAR(100) GENERATED ALWAYS AS (LOWER(name)),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL
);
CREATE UNIQUE INDEX period_sheet_name ON period (sheet_id, name_key);
