-- A sheet's rate for each grade: a percent with two decimals. A grade with no row has no rate.
CREATE TABLE rate (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    grade CHAR(1) NOT NULL,
    pct DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
