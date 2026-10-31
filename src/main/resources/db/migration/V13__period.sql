-- A sheet's periods: a name unique on the sheet and a from and to date, inclusive. No two periods
-- of a sheet overlap (checked when one is added), so a claim belongs to the one whose dates contain
-- its date, or to none.
CREATE TABLE period (
    sheet_id  VARCHAR(200) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    CHECK (from_date <= to_date),
    PRIMARY KEY (sheet_id, name)
);
