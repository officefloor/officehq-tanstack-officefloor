-- The sheet's holidays: a claim's due-by, when fixed at creation, is moved past any of them. Each date at
-- most once per sheet; the id orders them as they were added. Kept by version like the rest of the screen.
CREATE TABLE holiday (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id     VARCHAR(255) NOT NULL,
    holiday_date DATE         NOT NULL,
    UNIQUE (sheet_id, holiday_date)
);

CREATE TABLE holiday_hist AS SELECT * FROM holiday WITH NO DATA;
ALTER TABLE holiday_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE holiday_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE holiday_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX holiday_hist_key ON holiday_hist (id, hist_seq);
CREATE TRIGGER holiday_history AFTER INSERT, UPDATE, DELETE ON holiday FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
