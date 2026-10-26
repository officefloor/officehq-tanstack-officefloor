-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to claims added after it
-- was saved. Claims and rates take their place in the order of changes from one sequence: a claim's added_seq when it is
-- added, a forward-only rate's after_seq when it is saved (null: the rate applies to every claim). A forward-only rate
-- applies to a claim whose added_seq is above its after_seq; an older claim keeps the version it had before (rate_version),
-- or none. Claims added before this change have no added_seq and count as added before every forward-only rate.
CREATE SEQUENCE change_seq;
ALTER TABLE claim ADD COLUMN added_seq BIGINT DEFAULT NEXT VALUE FOR change_seq;
ALTER TABLE hist.claim ADD COLUMN added_seq BIGINT;
ALTER TABLE asof.claim ADD COLUMN added_seq BIGINT;
ALTER TABLE rate ADD COLUMN after_seq BIGINT;
ALTER TABLE hist.rate ADD COLUMN after_seq BIGINT;
ALTER TABLE asof.rate ADD COLUMN after_seq BIGINT;
ALTER TABLE rate_version ADD COLUMN after_seq BIGINT;

-- The older versions are read with the rates, so they are snapshotted with them.
CREATE TABLE hist.rate_version AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM rate_version t WHERE FALSE;
CREATE INDEX hist_rate_version_snapshot ON hist.rate_version (snapshot_id);
CREATE TABLE asof.rate_version AS SELECT * FROM rate_version WHERE FALSE;
