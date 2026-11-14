-- The sheet's rate for each grade: a percent (0.00 or more, two decimals). A grade with no row has no rate.
CREATE TABLE rate (
    sheet_id   VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    grade      VARCHAR(1)     NOT NULL,
    pct        DECIMAL(19, 2) NOT NULL CHECK (pct >= 0),
    set_by     VARCHAR(200)   NOT NULL,
    set_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sheet_id, grade)
);
