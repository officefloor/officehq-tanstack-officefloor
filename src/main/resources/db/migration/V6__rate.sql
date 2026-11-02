-- A sheet's rate for each grade: a percent with two decimals (0.00 or more). A grade has at most
-- one rate; setting it again replaces it. A grade with no row has no rate.
CREATE TABLE rate (
    sheet_id VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    grade    VARCHAR(1)    NOT NULL,
    pct      DECIMAL(7, 2) NOT NULL CHECK (pct >= 0),
    PRIMARY KEY (sheet_id, grade)
);
