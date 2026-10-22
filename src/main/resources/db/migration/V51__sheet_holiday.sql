-- The sheet's holidays: dates a new claim's due-by is moved past (to the next day that is not one) when it is fixed.
-- A date is on a sheet's list at most once.
CREATE TABLE sheet_holiday (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(100) NOT NULL REFERENCES sheet(id),
    holiday_date DATE         NOT NULL,
    added_by     VARCHAR(100) NOT NULL,
    CONSTRAINT sheet_holiday_date UNIQUE (sheet_id, holiday_date)
);
