-- The rate for each grade on a sheet: a percent, 0.00 or more, two decimals. A grade with no row
-- has no rate, so its claims' due is n/a. Setting a grade's rate again replaces it.
CREATE TABLE rate (
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(1)   NOT NULL,
    pct      DECIMAL(7,2) NOT NULL CHECK (pct >= 0),
    PRIMARY KEY (sheet_id, grade)
);
