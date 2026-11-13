-- The sheet's rate for each grade: a percent with two decimals. A grade with no row has no rate.
CREATE TABLE rate (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(10) NOT NULL,
    rate DECIMAL(7, 2) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
