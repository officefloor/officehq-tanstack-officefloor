-- A sheet as it stood at the end of a run of its todays: taken as the sheet's today moves on from `through` to
-- `until`, it holds every change made while the sheet's today was `through` or earlier, and none made later. A
-- statement as of a date reads the latest snapshot with through <= date < until (see SheetHistory).
CREATE TABLE sheet_snapshot (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    through  DATE         NOT NULL,
    until    DATE         NOT NULL
);
CREATE INDEX sheet_snapshot_sheet ON sheet_snapshot (sheet_id, id);

-- The snapshotted rows: each table a statement's figures read, with the snapshot's id last.
CREATE SCHEMA hist;
CREATE TABLE hist.sheet AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet t WHERE FALSE;
CREATE TABLE hist.person AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person t WHERE FALSE;
CREATE TABLE hist.person_grade AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_grade t WHERE FALSE;
CREATE TABLE hist.person_hold AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_hold t WHERE FALSE;
CREATE TABLE hist.person_patron AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_patron t WHERE FALSE;
CREATE TABLE hist.statement AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM statement t WHERE FALSE;
CREATE TABLE hist.claim AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim t WHERE FALSE;
CREATE TABLE hist.rate AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM rate t WHERE FALSE;
CREATE TABLE hist.sheet_setting AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet_setting t WHERE FALSE;
CREATE TABLE hist.sheet_setting_from AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet_setting_from t WHERE FALSE;
CREATE INDEX hist_sheet_snapshot ON hist.sheet (snapshot_id);
CREATE INDEX hist_person_snapshot ON hist.person (snapshot_id);
CREATE INDEX hist_person_grade_snapshot ON hist.person_grade (snapshot_id);
CREATE INDEX hist_person_hold_snapshot ON hist.person_hold (snapshot_id);
CREATE INDEX hist_person_patron_snapshot ON hist.person_patron (snapshot_id);
CREATE INDEX hist_statement_snapshot ON hist.statement (snapshot_id);
CREATE INDEX hist_claim_snapshot ON hist.claim (snapshot_id);
CREATE INDEX hist_rate_snapshot ON hist.rate (snapshot_id);
CREATE INDEX hist_sheet_setting_snapshot ON hist.sheet_setting (snapshot_id);
CREATE INDEX hist_sheet_setting_from_snapshot ON hist.sheet_setting_from (snapshot_id);

-- A snapshot laid out as the live tables, for the length of one request: the repositories read it unchanged with
-- this schema current, the sheet's today standing at the statement's date. Emptied again before the request ends.
CREATE SCHEMA asof;
CREATE TABLE asof.sheet AS SELECT * FROM sheet WHERE FALSE;
CREATE TABLE asof.person AS SELECT * FROM person WHERE FALSE;
CREATE TABLE asof.person_grade AS SELECT * FROM person_grade WHERE FALSE;
CREATE TABLE asof.person_hold AS SELECT * FROM person_hold WHERE FALSE;
CREATE TABLE asof.person_patron AS SELECT * FROM person_patron WHERE FALSE;
CREATE TABLE asof.statement AS SELECT * FROM statement WHERE FALSE;
CREATE TABLE asof.claim AS SELECT * FROM claim WHERE FALSE;
CREATE TABLE asof.rate AS SELECT * FROM rate WHERE FALSE;
CREATE TABLE asof.sheet_setting AS SELECT * FROM sheet_setting WHERE FALSE;
CREATE TABLE asof.sheet_setting_from AS SELECT * FROM sheet_setting_from WHERE FALSE;
