-- Sheet-wide numbering restarts each fiscal year: a claim opened from now on is numbered
-- FY<yyyy>-<n>, the sequence of each fiscal year shared by the sheets of a tree. The last number given
-- in each fiscal year's sequence on a sheet (the next is one after the last on any sheet of the tree);
-- numbers already given (and sheet.last_claim_number) stay.
CREATE TABLE sheet_fy_number (
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    fy          VARCHAR(20)  NOT NULL,
    last_number BIGINT       DEFAULT 0 NOT NULL,
    PRIMARY KEY (sheet_id, fy)
);
