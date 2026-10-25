-- The sheet's periods: a name, unique on the sheet ignoring case and surrounding spaces (the app
-- refuses a duplicate with a message, the index is the backstop), and a from and to date, both
-- inclusive. No two periods on a sheet overlap; a claim belongs to the period containing its date.
CREATE TABLE period (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name      VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    name_key  VARCHAR(200) GENERATED ALWAYS AS (LOWER(TRIM(name))),
    CHECK (from_date <= to_date)
);
CREATE UNIQUE INDEX period_sheet_name ON period (sheet_id, name_key);
