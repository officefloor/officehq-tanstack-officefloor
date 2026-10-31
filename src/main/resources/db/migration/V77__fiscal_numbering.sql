-- How many sheet-wide numbers each sheet has given out within a fiscal year (`fy` is the year,
-- yyyy, of FY<yyyy>): a claim opened sheet-wide is numbered FY<yyyy>-<n>, the sheets of a tree
-- sharing one sequence per fiscal year. Numbers given before this (plain "7") stay as they are.
CREATE TABLE fiscal_number (
    sheet_id VARCHAR(200) NOT NULL,
    fy       INT          NOT NULL,
    given    BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, fy)
);
