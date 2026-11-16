-- The last number given in each fiscal year's sheet-wide sequence of a sheet: fiscal_year is FY<yyyy>, the fiscal
-- year (by the sheet's fiscal start month) of the due-by of the claims numbered in it. A tree shares each fiscal
-- year's sequence: the next number is one more than the last given on any sheet of the tree. Like claim_number_seq,
-- a number is never given twice.
CREATE TABLE claim_fy_number_seq (
    sheet_id    VARCHAR(200) NOT NULL,
    fiscal_year VARCHAR(20) NOT NULL,
    last_number BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, fiscal_year)
);
