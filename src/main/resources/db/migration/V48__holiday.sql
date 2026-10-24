-- A sheet's holidays: dates, each on the sheet at most once. A claim's due-by, fixed when it is added, is moved
-- past the holidays on the sheet then.
CREATE TABLE holiday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    holiday_date DATE NOT NULL
);
CREATE UNIQUE INDEX holiday_sheet_date ON holiday (sheet_id, holiday_date);
