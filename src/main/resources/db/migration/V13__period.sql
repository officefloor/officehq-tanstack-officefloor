-- A sheet's periods: a name unique on the sheet and a from and to date, inclusive. No two periods
-- of a sheet overlap (checked when one is added).
CREATE TABLE period (
    sheet_id  VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    CHECK (from_date <= to_date),
    PRIMARY KEY (sheet_id, name)
);
