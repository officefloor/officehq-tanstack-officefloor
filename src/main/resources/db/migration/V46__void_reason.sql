-- The sheet's void reasons: while a sheet has any, a claim is voided with one of them. Each reason (trimmed)
-- at most once per sheet; the id orders them as they were added. Kept by version like the rest of the screen.
CREATE TABLE void_reason (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255)  NOT NULL,
    reason   VARCHAR(1000) NOT NULL,
    UNIQUE (sheet_id, reason)
);

CREATE TABLE void_reason_hist AS SELECT * FROM void_reason WITH NO DATA;
ALTER TABLE void_reason_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE void_reason_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE void_reason_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX void_reason_hist_key ON void_reason_hist (id, hist_seq);
CREATE TRIGGER void_reason_history AFTER INSERT, UPDATE, DELETE ON void_reason FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

-- The reason a claim was voided with (null when voided while the sheet had none). Kept in claim_hist too;
-- the history trigger is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(1000);
ALTER TABLE claim_hist ADD COLUMN void_reason VARCHAR(1000);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
