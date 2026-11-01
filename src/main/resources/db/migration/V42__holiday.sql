-- The sheet's holidays: each date at most once. A due-by fixed at a claim's creation is moved past them.
CREATE TABLE holiday (
    sheet      VARCHAR(200) NOT NULL,
    holiday_on DATE         NOT NULL,
    PRIMARY KEY (sheet, holiday_on)
);
