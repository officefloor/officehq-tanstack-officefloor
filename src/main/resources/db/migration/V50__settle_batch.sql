-- The bulk settle batch a claim was settled in (1, 2, ... on its sheet); null when it was settled one at
-- a time, or not settled. Kept in claim_hist too; the history trigger is set again afterwards, as in
-- V45__claim_tags.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN settle_batch BIGINT;
ALTER TABLE claim_hist ADD COLUMN settle_batch BIGINT;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
-- The last batch given on each sheet: a bulk settle that settles at least one claim takes the next.
-- Its row is locked while a batch is taken, so two bulk settles at once get different batches.
CREATE TABLE settle_batch (
    sheet_id   VARCHAR(255) PRIMARY KEY,
    last_batch BIGINT NOT NULL
);
