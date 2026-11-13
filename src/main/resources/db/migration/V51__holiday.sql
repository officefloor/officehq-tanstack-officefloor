-- A holiday on a sheet: a claim's due-by, fixed when it is created, that would fall on one moves to the next day
-- that is not one. A date is on a sheet's list at most once.
CREATE TABLE holiday (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    holiday_date DATE NOT NULL,
    PRIMARY KEY (sheet_id, holiday_date)
);
