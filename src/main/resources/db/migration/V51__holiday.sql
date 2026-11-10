-- A sheet's holidays: a date at most once per sheet. A claim's due-by fixed at its creation is moved
-- past any holiday on the sheet then.
CREATE TABLE sheet_holiday (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    holiday_date DATE NOT NULL,
    PRIMARY KEY (sheet_id, holiday_date)
);
