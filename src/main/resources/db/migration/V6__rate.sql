-- The sheet's rate for each grade: a percent with two decimals (0.00 or more). A grade has at most
-- one rate on a sheet; setting it again replaces it. A grade with no rate has no due.
CREATE TABLE rate (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100)  NOT NULL,
    grade    VARCHAR(1)    NOT NULL,
    rate     DECIMAL(7, 2) NOT NULL
);
CREATE UNIQUE INDEX rate_sheet_grade ON rate (sheet_id, grade);
