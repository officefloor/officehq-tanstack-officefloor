-- The as-of view: the whole screen is shown as the sheet stood at the end of a date, so a snapshot (see V40) holds
-- every table the screen reads, not only those a statement's figures read.
CREATE TABLE hist.person_watch AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_watch t WHERE FALSE;
CREATE TABLE hist.claim_log AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim_log t WHERE FALSE;
CREATE TABLE hist.claim_lock AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM claim_lock t WHERE FALSE;
CREATE TABLE hist.period AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM period t WHERE FALSE;
CREATE TABLE hist.period_person_total AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM period_person_total t WHERE FALSE;
CREATE TABLE hist.sheet_role AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet_role t WHERE FALSE;
CREATE TABLE hist.sheet_delegation AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM sheet_delegation t WHERE FALSE;
CREATE INDEX hist_person_watch_snapshot ON hist.person_watch (snapshot_id);
CREATE INDEX hist_claim_log_snapshot ON hist.claim_log (snapshot_id);
CREATE INDEX hist_claim_lock_snapshot ON hist.claim_lock (snapshot_id);
CREATE INDEX hist_period_snapshot ON hist.period (snapshot_id);
CREATE INDEX hist_period_person_total_snapshot ON hist.period_person_total (snapshot_id);
CREATE INDEX hist_sheet_role_snapshot ON hist.sheet_role (snapshot_id);
CREATE INDEX hist_sheet_delegation_snapshot ON hist.sheet_delegation (snapshot_id);

CREATE TABLE asof.person_watch AS SELECT * FROM person_watch WHERE FALSE;
CREATE TABLE asof.claim_log AS SELECT * FROM claim_log WHERE FALSE;
CREATE TABLE asof.claim_lock AS SELECT * FROM claim_lock WHERE FALSE;
CREATE TABLE asof.period AS SELECT * FROM period WHERE FALSE;
CREATE TABLE asof.period_person_total AS SELECT * FROM period_person_total WHERE FALSE;
CREATE TABLE asof.sheet_role AS SELECT * FROM sheet_role WHERE FALSE;
CREATE TABLE asof.sheet_delegation AS SELECT * FROM sheet_delegation WHERE FALSE;
