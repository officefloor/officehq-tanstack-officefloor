-- Forward-only rates. Every claim takes the next value of change_seq when it is added (by any path), kept as
-- added_seq (a copy of a claim, on a clone or a transfer, keeps its original's); claims already there take 0.
-- A rate saved with a from-date on or after the sheet's today takes the next value too, as forward_seq, and
-- applies only to claims with a later added_seq; a rate with none (null) applies to every claim. Kept in
-- claim_hist and rate_hist too; the history triggers are set again afterwards, as in V70__rate_versions.sql.
CREATE SEQUENCE change_seq;

DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN added_seq BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE claim ALTER COLUMN added_seq SET DEFAULT NEXT VALUE FOR change_seq;
ALTER TABLE claim_hist ADD COLUMN added_seq BIGINT DEFAULT 0 NOT NULL;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

DROP TRIGGER rate_history;
ALTER TABLE rate ADD COLUMN forward_seq BIGINT;
ALTER TABLE rate_hist ADD COLUMN forward_seq BIGINT;
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON rate FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
ALTER TABLE rate_version ADD COLUMN forward_seq BIGINT;
