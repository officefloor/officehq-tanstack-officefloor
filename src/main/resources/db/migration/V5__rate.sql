-- The sheet's rate for each grade: a percent with two decimals. A grade without a row has no rate.
CREATE TABLE rate (
    sheet_id VARCHAR(100)  NOT NULL,
    grade    CHAR(1)       NOT NULL CHECK (grade IN ('A', 'B', 'C')),
    pct      DECIMAL(5, 2) NOT NULL CHECK (pct >= 0 AND pct <= 100),
    PRIMARY KEY (sheet_id, grade)
);
