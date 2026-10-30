-- A sheet's periods: a name unique on the sheet and a from and to date, both inclusive. No two
-- periods on a sheet overlap, so a claim belongs to the one period whose dates contain its date,
-- or to none.
CREATE TABLE period (
    sheet_id  VARCHAR(200) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
