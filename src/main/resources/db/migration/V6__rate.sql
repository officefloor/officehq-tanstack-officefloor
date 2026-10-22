-- The sheet's rate for each grade: a percent with two decimals. A grade with no row has no rate.
CREATE TABLE rate (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet(id),
    grade    CHAR(1)      NOT NULL CHECK (grade IN ('A', 'B', 'C')),
    pct      DECIMAL(5, 2) NOT NULL CHECK (pct >= 0),
    PRIMARY KEY (sheet_id, grade)
);
