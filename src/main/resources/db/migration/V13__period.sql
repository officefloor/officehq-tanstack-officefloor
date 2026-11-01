-- The sheet's periods: a name unique on the sheet and a from and to date, inclusive. No two periods of a
-- sheet overlap; a claim belongs to the period whose dates contain its date, or to none.
CREATE TABLE period (
    sheet     VARCHAR(200) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL,
    PRIMARY KEY (sheet, name)
);
