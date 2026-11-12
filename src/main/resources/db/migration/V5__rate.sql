-- The sheet's rate for each grade: a percent with two decimals. A grade with no row has no rate.
CREATE TABLE rate (
    sheet_id VARCHAR(255)   NOT NULL,
    grade    VARCHAR(1)     NOT NULL,
    pct      DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
