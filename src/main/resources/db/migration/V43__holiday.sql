-- The sheet's holidays: each date at most once on a sheet (the app refuses a duplicate with a message, the index is
-- the backstop). A claim's due-by, fixed when it is added, is moved past the holidays on the sheet then.
CREATE TABLE holiday (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(200) NOT NULL REFERENCES sheet (id),
    holiday_date DATE         NOT NULL
);
CREATE UNIQUE INDEX holiday_sheet_date ON holiday (sheet_id, holiday_date);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.holiday AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM holiday t WHERE FALSE;
CREATE INDEX hist_holiday_snapshot ON hist.holiday (snapshot_id);
CREATE TABLE asof.holiday AS SELECT * FROM holiday WHERE FALSE;
