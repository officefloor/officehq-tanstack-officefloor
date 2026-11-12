-- A claim's tags: each trimmed and lower-cased, repeats dropped, sorted and comma separated (no spaces);
-- null when it has none. A claim split off another carries the original's. Kept in claim_hist too (see
-- V39__history.sql); the history trigger is set again afterwards, as in V41__claim_reminded.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN tags VARCHAR(4000);
ALTER TABLE claim_hist ADD COLUMN tags VARCHAR(4000);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
