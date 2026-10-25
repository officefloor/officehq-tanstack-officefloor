-- The sheet's void reasons, in the order added: each trimmed reason at most once on a sheet (the app refuses a repeat
-- with a message, the index is the backstop). While a sheet has any, a void needs one of them.
CREATE TABLE void_reason (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL REFERENCES sheet (id),
    reason   VARCHAR(1000) NOT NULL
);
CREATE UNIQUE INDEX void_reason_sheet_reason ON void_reason (sheet_id, reason);

-- The reason a claim was voided with (null: none given).
ALTER TABLE claim ADD COLUMN void_reason VARCHAR(1000);
ALTER TABLE hist.claim ADD COLUMN void_reason VARCHAR(1000);
ALTER TABLE asof.claim ADD COLUMN void_reason VARCHAR(1000);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.void_reason AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM void_reason t WHERE FALSE;
CREATE INDEX hist_void_reason_snapshot ON hist.void_reason (snapshot_id);
CREATE TABLE asof.void_reason AS SELECT * FROM void_reason WHERE FALSE;
