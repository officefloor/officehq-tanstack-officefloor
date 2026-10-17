-- The sheet's periods: a name, unique on the sheet, and a from and to date, inclusive (from on or
-- before to). No two periods of a sheet overlap. A claim belongs to the period whose dates contain
-- its date, or to none.
CREATE TABLE period (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL,
    name      VARCHAR(100) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE UNIQUE INDEX period_sheet_name ON period (sheet_id, name);
