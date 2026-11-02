-- Sheet-wide numbering restarts each fiscal year: a claim opened outside period numbering shows FY<yyyy>-<n>,
-- kept like a period number (in claim.period_number, the number it shows). The last number given in each
-- fiscal year on each sheet; the sequence of a fiscal year is shared across the sheet's tree.
CREATE TABLE claim_fy_number (
    sheet       VARCHAR(200) NOT NULL,
    fy          VARCHAR(20)  NOT NULL,
    last_number INT NOT NULL,
    PRIMARY KEY (sheet, fy)
);
