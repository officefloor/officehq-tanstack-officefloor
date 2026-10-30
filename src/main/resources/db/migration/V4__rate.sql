-- The rate for each grade on a sheet: a percent with two decimals. A grade with no row has no rate,
-- so its claims' due and its people's due-total are n/a.
CREATE TABLE rate (
    sheet_id VARCHAR(200) NOT NULL,
    grade    CHAR(1)      NOT NULL,
    rate     DECIMAL(6,2) NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
