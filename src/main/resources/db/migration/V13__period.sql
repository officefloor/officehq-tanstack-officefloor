-- The periods of a sheet, listed by from-date. name: unique on the sheet, compared without regard to
-- case and stored without surrounding spaces; from_date to to_date, both inclusive, from <= to. No
-- two periods of a sheet overlap. A claim belongs to the period whose dates contain its date, or none.
CREATE TABLE period (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    name_key  VARCHAR(200) GENERATED ALWAYS AS (LOWER(name)),
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    CHECK (from_date <= to_date)
);
CREATE UNIQUE INDEX period_sheet_name ON period (sheet_id, name_key);
