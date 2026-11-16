-- The sheet's holidays: each date at most once. A claim's due-by falling on one is moved, as the claim is added, to the
-- next day that is not a holiday.
CREATE TABLE holiday (
    sheet_id     VARCHAR(200) NOT NULL,
    holiday_date DATE         NOT NULL,
    PRIMARY KEY (sheet_id, holiday_date)
);
