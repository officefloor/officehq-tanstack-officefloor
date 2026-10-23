-- Sheet-wide numbering restarts each fiscal year: a claim opened outside period numbering is numbered
-- FY<yyyy>-<n>, counting from 1 within the fiscal year (by the opening sheet's fiscal year start month) of its
-- due-by, the sequence shared across the sheet's tree. Numbers already given (plain digits) stay.
-- The last number given in each fiscal year's sequence on a sheet, by the fiscal year's first calendar year.
CREATE TABLE fiscal_year_number (
    sheet_id    VARCHAR(100) NOT NULL REFERENCES sheet(id),
    fiscal_year INT          NOT NULL,
    last_number INT          NOT NULL,
    PRIMARY KEY (sheet_id, fiscal_year)
);
