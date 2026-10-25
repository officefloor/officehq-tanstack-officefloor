-- The sheet's rate for each grade: a percent with two decimals (0.00 to 100.00). A grade without
-- a row has no rate, so its claims' due shows n/a.
CREATE TABLE rate (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade    VARCHAR(1)   NOT NULL,
    rate     DECIMAL(5, 2) NOT NULL CHECK (rate >= 0 AND rate <= 100),
    PRIMARY KEY (sheet_id, grade)
);
