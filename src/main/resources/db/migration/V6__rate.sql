-- A sheet's rate for a grade: a percent with two decimals (0.00 or more), one per grade. A grade with
-- no row has no rate.
CREATE TABLE rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(1) NOT NULL,
    pct DECIMAL(19, 2) NOT NULL
);
CREATE UNIQUE INDEX rate_sheet_grade ON rate (sheet_id, grade);
