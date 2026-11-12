-- The sheet's periods: each named (unique on the sheet, ignoring case) and spanning from_date to
-- to_date inclusive; no two periods on a sheet overlap. Listed by from-date.
CREATE TABLE period (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    from_date DATE         NOT NULL,
    to_date   DATE         NOT NULL
);
CREATE INDEX period_sheet ON period (sheet_id, from_date);
