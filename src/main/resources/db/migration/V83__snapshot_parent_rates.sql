-- A sheet's parents' rates as they stood when the sheet was snapshotted: the sheet's parent chain then (each parent at
-- its level up, 1 the parent) and each parent's rates and older versions. Viewed as of a date the snapshot holds, the
-- sheet's parents' rates are these, as they stood then.
CREATE TABLE hist.snapshot_parent (
    snapshot_id BIGINT       NOT NULL,
    level       INT          NOT NULL,
    sheet_id    VARCHAR(200) NOT NULL
);
CREATE INDEX hist_snapshot_parent_snapshot ON hist.snapshot_parent (snapshot_id);
CREATE TABLE hist.parent_rate AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM rate t WHERE FALSE;
CREATE INDEX hist_parent_rate_snapshot ON hist.parent_rate (snapshot_id);
CREATE TABLE hist.parent_rate_version AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM rate_version t WHERE FALSE;
CREATE INDEX hist_parent_rate_version_snapshot ON hist.parent_rate_version (snapshot_id);
