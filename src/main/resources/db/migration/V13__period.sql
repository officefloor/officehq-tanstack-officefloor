-- The sheet's periods: a name unique on the sheet (case ignored) and a from and to date, both inclusive. No two
-- periods of a sheet overlap; the app checks that before it adds one. A claim belongs to the period whose dates
-- contain its date, or to none. The id orders them as added.
CREATE TABLE period (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE INDEX period_sheet ON period (sheet_id, from_date);
