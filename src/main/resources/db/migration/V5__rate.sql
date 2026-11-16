-- The sheet's rate for each grade: a percent kept exactly with two decimals. A grade with no row
-- here has no rate (its claims' due is n/a).
CREATE TABLE rate (
    sheet_id VARCHAR(200)  NOT NULL,
    grade    VARCHAR(1)    NOT NULL,
    rate     DECIMAL(7, 2) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
