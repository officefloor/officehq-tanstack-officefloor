-- The sheet's policy versions: every change to one of its policy settings (rounding, floor, cap, terms,
-- late-pct, clerk-limit) is a new version, numbered from 1 per sheet in the order made, with the key
-- and value it set and its from-date (null for none: in force on every date). The version in force on a
-- date is the number of versions whose from-date is on or before it (or have none).
CREATE TABLE policy_version (
    sheet_id      VARCHAR(200)  NOT NULL,
    version       INT           NOT NULL,
    setting_key   VARCHAR(20)   NOT NULL,
    from_date     DATE,
    setting_value DECIMAL(12,2),
    PRIMARY KEY (sheet_id, version)
);

CREATE TABLE asof.policy_version AS SELECT * FROM public.policy_version WHERE FALSE;
CREATE TABLE asof_hist.policy_version AS SELECT * FROM public.policy_version WHERE FALSE;
ALTER TABLE asof_hist.policy_version ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.policy_version ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.policy_version ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX policy_version_hist_sheet ON asof_hist.policy_version (sheet_id, h_seq);
CREATE TRIGGER policy_version_history AFTER INSERT, UPDATE, DELETE ON public.policy_version FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
