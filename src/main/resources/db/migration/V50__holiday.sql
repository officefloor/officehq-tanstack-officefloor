-- The sheet's holidays: a claim's due-by, when it is fixed at creation, that falls on one is moved
-- to the next day that is not one. A date is on a sheet's list at most once.
CREATE TABLE sheet_holiday (
    sheet_id     VARCHAR(200) NOT NULL,
    holiday_date DATE         NOT NULL,
    PRIMARY KEY (sheet_id, holiday_date)
);

CREATE TABLE asof.sheet_holiday AS SELECT * FROM public.sheet_holiday WHERE FALSE;
CREATE TABLE asof_hist.sheet_holiday AS SELECT * FROM public.sheet_holiday WHERE FALSE;
ALTER TABLE asof_hist.sheet_holiday ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_holiday ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_holiday ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_holiday_hist_sheet ON asof_hist.sheet_holiday (sheet_id, h_seq);
CREATE TRIGGER sheet_holiday_history AFTER INSERT, UPDATE, DELETE ON public.sheet_holiday FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
