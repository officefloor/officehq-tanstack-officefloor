-- A sheet's periods: a name unique on the sheet, and a from and to date, inclusive. No two of a
-- sheet's periods overlap (checked when one is added).
CREATE TABLE period (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    name VARCHAR(200) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
