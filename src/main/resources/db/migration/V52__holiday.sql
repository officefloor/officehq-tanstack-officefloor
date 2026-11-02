-- The sheet's holidays: a claim's due-by, fixed when it is created, that falls on one is moved to the next
-- day that is not a holiday. Each date is on the sheet's list at most once.
CREATE TABLE holiday (
    sheet_id     VARCHAR(200) NOT NULL REFERENCES sheet (id),
    holiday_date DATE         NOT NULL,
    PRIMARY KEY (sheet_id, holiday_date)
);
