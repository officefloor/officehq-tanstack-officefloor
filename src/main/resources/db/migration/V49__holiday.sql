-- The sheet's holidays: a claim's due-by fixed at creation that falls on one moves to the next day that is not.
CREATE TABLE holiday (
    sheet_id     VARCHAR(200) NOT NULL,
    holiday_date DATE         NOT NULL,
    PRIMARY KEY (sheet_id, holiday_date)
);
